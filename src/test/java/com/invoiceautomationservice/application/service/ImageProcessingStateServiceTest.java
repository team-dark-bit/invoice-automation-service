package com.invoiceautomationservice.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.invoiceautomationservice.application.port.out.ConversationContextRepository;
import com.invoiceautomationservice.application.port.out.ConversationImageRepository;
import com.invoiceautomationservice.application.port.out.ConversationRepository;
import com.invoiceautomationservice.domain.model.Conversation;
import com.invoiceautomationservice.domain.model.ConversationChannel;
import com.invoiceautomationservice.domain.model.ConversationContext;
import com.invoiceautomationservice.domain.model.ConversationFlowState;
import com.invoiceautomationservice.domain.model.ConversationImage;
import com.invoiceautomationservice.domain.model.DocumentInterpretation;
import com.invoiceautomationservice.domain.model.ImageFormat;
import com.invoiceautomationservice.domain.model.ImageProcessingStatus;
import com.invoiceautomationservice.domain.model.ImageRetentionPolicy;
import com.invoiceautomationservice.domain.model.InterpretationSource;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ImageProcessingStateServiceTest {
  private static final Instant NOW = Instant.parse("2026-10-02T18:00:00Z");
  private ConversationImageRepository images;
  private ConversationContextRepository contexts;
  private ImageProcessingStateService service;
  private ConversationImage processing;

  @BeforeEach
  void setUp() {
    images = mock(ConversationImageRepository.class);
    contexts = mock(ConversationContextRepository.class);
    ConversationRepository conversations = mock(ConversationRepository.class);
    processing = processingImage();
    Conversation conversation = Conversation.open("company-1", null, null,
        ConversationChannel.REST, "contact", NOW.minusSeconds(60));
    when(images.findByIdForUpdate(processing.id())).thenReturn(Optional.of(processing));
    when(images.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    when(conversations.findById(processing.conversationId())).thenReturn(conversation);
    when(conversations.findByIdForUpdate(processing.conversationId())).thenReturn(conversation);
    when(contexts.findByConversationId(processing.conversationId())).thenReturn(Optional.empty());
    when(contexts.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    service = new ImageProcessingStateService(images, conversations, contexts,
        mock(AuditTrailService.class), Clock.fixed(NOW, ZoneOffset.UTC));
  }

  @Test
  void extractedImageLeavesDetectedValuesPendingReview() {
    DocumentInterpretation interpretation = DocumentInterpretation.unsupported(
        InterpretationSource.IMAGE, "manual review required");

    service.complete(processing.id(), interpretation);

    ArgumentCaptor<ConversationContext> context =
        ArgumentCaptor.forClass(ConversationContext.class);
    verify(contexts).save(context.capture());
    assertThat(context.getValue().state()).isEqualTo(ConversationFlowState.NEEDS_REVIEW);
    assertThat(context.getValue().reviewRequired()).isTrue();
    assertThat(context.getValue().lastInterpretation()).isEqualTo(interpretation);
  }

  @Test
  void failedImageProcessingAlsoEndsInReviewWithoutConfirmedValues() {
    service.fail(processing.id(), "Multimodal processing failed");

    ArgumentCaptor<ConversationContext> context =
        ArgumentCaptor.forClass(ConversationContext.class);
    verify(contexts).save(context.capture());
    assertThat(context.getValue().state()).isEqualTo(ConversationFlowState.NEEDS_REVIEW);
    assertThat(context.getValue().documentType()).isNull();
    assertThat(context.getValue().lastInterpretation().source())
        .isEqualTo(InterpretationSource.IMAGE);
  }

  private ConversationImage processingImage() {
    return new ConversationImage(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
        "2026/10/image.png", "receipt.png", ImageFormat.PNG, 128, 10, 20, "a".repeat(64),
        ImageRetentionPolicy.TEMPORARY, NOW.plusSeconds(3600), NOW.minusSeconds(2), null,
        ImageProcessingStatus.PROCESSING, 1, NOW.minusSeconds(1), null, null, null);
  }
}
