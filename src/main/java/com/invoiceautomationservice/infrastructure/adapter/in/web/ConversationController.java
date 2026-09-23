package com.invoiceautomationservice.infrastructure.adapter.in.web;

import com.invoiceautomationservice.application.dto.request.CreateConversationRequest;
import com.invoiceautomationservice.application.dto.request.CreateMessageRequest;
import com.invoiceautomationservice.application.dto.response.ConversationResponse;
import com.invoiceautomationservice.application.dto.response.MessageResponse;
import com.invoiceautomationservice.application.dto.response.PageResponse;
import com.invoiceautomationservice.application.service.ConversationService;
import com.invoiceautomationservice.commons.response.ApiResponse;
import com.invoiceautomationservice.domain.model.ConversationStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/conversations")
@RequiredArgsConstructor
@Validated
public class ConversationController {
  private final ConversationService service;

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
}
