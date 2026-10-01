package com.invoiceautomationservice.infrastructure.adapter.in.web;

import com.invoiceautomationservice.application.dto.request.CreateConversationRequest;
import com.invoiceautomationservice.application.dto.request.CreateMessageRequest;
import com.invoiceautomationservice.application.dto.response.ConversationResponse;
import com.invoiceautomationservice.application.dto.response.MessageResponse;
import com.invoiceautomationservice.application.dto.response.PageResponse;
import com.invoiceautomationservice.application.service.ConversationService;
import com.invoiceautomationservice.application.port.in.ConversationEngineUseCase;
import com.invoiceautomationservice.application.dto.request.ProcessConversationRequest;
import com.invoiceautomationservice.application.dto.response.ConversationEngineResponse;
import com.invoiceautomationservice.application.dto.response.ConversationImageResponse;
import com.invoiceautomationservice.application.model.UploadConversationImageCommand;
import com.invoiceautomationservice.application.port.in.ConversationImageUseCase;
import com.invoiceautomationservice.commons.response.ApiResponse;
import com.invoiceautomationservice.domain.model.ConversationStatus;
import com.invoiceautomationservice.domain.model.ImageRetentionPolicy;
import com.invoiceautomationservice.infrastructure.config.exception.ApplicationException;
import static com.invoiceautomationservice.infrastructure.config.exception.RuntimeErrors.IMAGE_STORAGE_FAILURE;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/conversations")
@RequiredArgsConstructor
@Validated
public class ConversationController {
  private final ConversationService service;
  private final ConversationEngineUseCase engine;
  private final ConversationImageUseCase images;

  @PostMapping
  public ResponseEntity<ApiResponse<ConversationResponse>> create(
      @RequestHeader(value = "X-Company-Id", required = false) String companyId,
      @RequestBody @Valid CreateConversationRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
        201, "Conversation created", service.create(companyId, request)));
  }

  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<ConversationResponse>> findById(@PathVariable UUID id) {
    return ResponseEntity.ok(ApiResponse.success(200, "Conversation found", service.findById(id)));
  }

  @GetMapping
  public ResponseEntity<ApiResponse<PageResponse<ConversationResponse>>> search(
      @RequestHeader(value = "X-Company-Id", required = false) String companyId,
      @RequestParam(required = false) ConversationStatus status,
      @RequestParam(required = false) String externalParticipantId,
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
    return ResponseEntity.ok(ApiResponse.success(200, "Conversations found",
        service.search(companyId, status, externalParticipantId, page, size)));
  }

  @PostMapping("/{id}/messages")
  public ResponseEntity<ApiResponse<MessageResponse>> addMessage(
      @PathVariable UUID id, @RequestBody @Valid CreateMessageRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
        201, "Message added", service.addMessage(id, request)));
  }

  @GetMapping("/{id}/messages")
  public ResponseEntity<ApiResponse<PageResponse<MessageResponse>>> findMessages(
      @PathVariable UUID id,
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "50") @Min(1) @Max(100) int size) {
    return ResponseEntity.ok(ApiResponse.success(200, "Messages found",
        service.findMessages(id, page, size)));
  }

  @PostMapping("/{id}/close")
  public ResponseEntity<ApiResponse<ConversationResponse>> close(@PathVariable UUID id) {
    return ResponseEntity.ok(ApiResponse.success(200, "Conversation closed", service.close(id)));
  }

  @PostMapping("/{id}/process")
  public ResponseEntity<ApiResponse<ConversationEngineResponse>> process(
      @PathVariable UUID id, @RequestBody @Valid ProcessConversationRequest request) {
    return ResponseEntity.ok(ApiResponse.success(200, "Message processed",
        engine.process(id, request.text(), request.externalMessageId())));
  }

  @PostMapping(path = "/{id}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<ApiResponse<ConversationImageResponse>> uploadImage(
      @PathVariable UUID id,
      @RequestPart("file") MultipartFile file,
      @RequestParam(required = false) String externalMessageId,
      @RequestParam(defaultValue = "TEMPORARY") ImageRetentionPolicy retentionPolicy,
      @RequestParam(required = false) Integer retentionDays) {
    ConversationImageResponse response;
    try {
      response = images.upload(id, new UploadConversationImageCommand(file.getOriginalFilename(),
          file.getContentType(), file.getBytes(), externalMessageId, retentionPolicy,
          retentionDays));
    } catch (IOException exception) {
      throw new ApplicationException(IMAGE_STORAGE_FAILURE);
    }
    int status = response.duplicate() ? 200 : 201;
    return ResponseEntity.status(status).body(ApiResponse.success(status,
        response.duplicate() ? "Image already received" : "Image stored", response));
  }

  @GetMapping("/{id}/images/{imageId}")
  public ResponseEntity<ApiResponse<ConversationImageResponse>> findImage(
      @PathVariable UUID id, @PathVariable UUID imageId) {
    return ResponseEntity.ok(ApiResponse.success(200, "Image found",
        images.findById(id, imageId)));
  }

  @GetMapping("/{id}/images/{imageId}/content")
  public ResponseEntity<byte[]> downloadImage(
      @PathVariable UUID id, @PathVariable UUID imageId) {
    var content = images.loadContent(id, imageId);
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(content.format().mediaType()))
        .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
            .filename(content.filename(), java.nio.charset.StandardCharsets.UTF_8).build().toString())
        .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
        .body(content.content());
  }

  @DeleteMapping("/{id}/images/{imageId}")
  public ResponseEntity<Void> deleteImage(
      @PathVariable UUID id, @PathVariable UUID imageId) {
    images.delete(id, imageId);
    return ResponseEntity.noContent().build();
  }
}
