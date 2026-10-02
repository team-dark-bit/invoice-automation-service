package com.invoiceautomationservice.infrastructure.inbound.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.invoiceautomationservice.application.dto.response.ConversationImageResponse;
import com.invoiceautomationservice.application.model.UploadConversationImageCommand;
import com.invoiceautomationservice.application.port.in.ConversationEngineUseCase;
import com.invoiceautomationservice.application.port.in.ConversationImageUseCase;
import com.invoiceautomationservice.application.service.ConversationService;
import com.invoiceautomationservice.domain.model.ImageRetentionPolicy;
import com.invoiceautomationservice.domain.model.ImageProcessingStatus;
import com.invoiceautomationservice.infrastructure.adapter.in.web.ConversationController;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockMultipartFile;

@WebMvcTest(ConversationController.class)
class ConversationControllerTest {
  @Autowired MockMvc mockMvc;
  @MockitoBean ConversationService conversationService;
  @MockitoBean ConversationEngineUseCase engine;
  @MockitoBean ConversationImageUseCase images;

  @Test
  void uploadsMultipartImage() throws Exception {
    UUID conversationId = UUID.randomUUID();
    UUID imageId = UUID.randomUUID();
    var response = new ConversationImageResponse(imageId, conversationId, UUID.randomUUID(),
        "ticket.png", "image/png", 100, 20, 10, "a".repeat(64),
        ImageRetentionPolicy.TEMPORARY, Instant.parse("2026-11-01T00:00:00Z"),
        Instant.parse("2026-10-01T00:00:00Z"), null, false,
        ImageProcessingStatus.RECEIVED, 0, null, null, null, null);
    when(images.upload(eq(conversationId), any(UploadConversationImageCommand.class)))
        .thenReturn(response);
    MockMultipartFile file = new MockMultipartFile(
        "file", "ticket.png", "image/png", new byte[]{1, 2, 3});

    mockMvc.perform(multipart("/api/v1/conversations/{id}/images", conversationId)
            .file(file)
            .param("externalMessageId", "media-1")
            .param("retentionPolicy", "TEMPORARY")
            .param("retentionDays", "30"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.statusCode").value(201))
        .andExpect(jsonPath("$.data.id").value(imageId.toString()))
        .andExpect(jsonPath("$.data.contentType").value("image/png"));
  }

  @Test
  void returnsOkWhenContentWasAlreadyReceived() throws Exception {
    UUID conversationId = UUID.randomUUID();
    var response = new ConversationImageResponse(UUID.randomUUID(), conversationId,
        UUID.randomUUID(), "ticket.png", "image/png", 100, 20, 10, "a".repeat(64),
        ImageRetentionPolicy.PERMANENT, null, Instant.parse("2026-10-01T00:00:00Z"),
        null, true, ImageProcessingStatus.RECEIVED, 0, null, null, null, null);
    when(images.upload(eq(conversationId), any())).thenReturn(response);

    mockMvc.perform(multipart("/api/v1/conversations/{id}/images", conversationId)
            .file(new MockMultipartFile("file", "ticket.png", "image/png", new byte[]{1})))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.duplicate").value(true));
  }

  @Test
  void queuesFailedImageProcessingRetry() throws Exception {
    UUID conversationId = UUID.randomUUID();
    UUID imageId = UUID.randomUUID();
    var response = new ConversationImageResponse(imageId, conversationId, UUID.randomUUID(),
        "ticket.png", "image/png", 100, 20, 10, "a".repeat(64),
        ImageRetentionPolicy.TEMPORARY, Instant.parse("2026-11-01T00:00:00Z"),
        Instant.parse("2026-10-01T00:00:00Z"), null, false,
        ImageProcessingStatus.RECEIVED, 1, null, null, null, null);
    when(images.retryProcessing(conversationId, imageId)).thenReturn(response);

    mockMvc.perform(post(
            "/api/v1/conversations/{id}/images/{imageId}/process", conversationId, imageId))
        .andExpect(status().isAccepted())
        .andExpect(jsonPath("$.statusCode").value(202))
        .andExpect(jsonPath("$.data.processingStatus").value("RECEIVED"))
        .andExpect(jsonPath("$.data.processingAttempts").value(1));
  }
}
