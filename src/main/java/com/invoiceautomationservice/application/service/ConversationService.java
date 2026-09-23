package com.invoiceautomationservice.application.service;

import static com.invoiceautomationservice.infrastructure.config.exception.RuntimeErrors.CONVERSATION_RESOURCE_COMPANY_MISMATCH;

import com.invoiceautomationservice.application.dto.request.CreateConversationRequest;
import com.invoiceautomationservice.application.dto.request.CreateMessageRequest;
import com.invoiceautomationservice.application.dto.response.ConversationResponse;
import com.invoiceautomationservice.application.dto.response.MessageResponse;
import com.invoiceautomationservice.application.dto.response.PageResponse;
import com.invoiceautomationservice.application.model.PageQuery;
import com.invoiceautomationservice.application.port.out.ConversationRepository;
import com.invoiceautomationservice.application.port.out.CustomerRepository;
import com.invoiceautomationservice.application.port.out.InvoiceDraftRepository;
import com.invoiceautomationservice.application.port.out.MessageRepository;
import com.invoiceautomationservice.domain.model.AuditAction;
import com.invoiceautomationservice.domain.model.CompanyPermission;
import com.invoiceautomationservice.domain.model.Conversation;
import com.invoiceautomationservice.domain.model.ConversationStatus;
import com.invoiceautomationservice.domain.model.Message;
import com.invoiceautomationservice.infrastructure.config.exception.ApplicationException;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ConversationService {
  private final ConversationRepository conversationRepository;
  private final MessageRepository messageRepository;
  private final CustomerRepository customerRepository;
  private final InvoiceDraftRepository invoiceDraftRepository;
  private final CompanyAccessService accessService;
  private final AuditTrailService auditTrailService;
  private final Clock clock;

  @Transactional
  public ConversationResponse create(
      String requestedCompanyId, CreateConversationRequest request) {
    String companyId = accessService.resolveCompanyId(requestedCompanyId);
    accessService.requirePermission(companyId, CompanyPermission.CONVERSATION_MANAGE);
    validateReferences(companyId, request.customerId(), request.invoiceDraftId());
    Conversation conversation = Conversation.open(companyId, request.customerId(),
        request.invoiceDraftId(), request.channel(), request.externalParticipantId(),
        Instant.now(clock));
    Conversation saved = conversationRepository.save(conversation);
    auditTrailService.record(companyId, AuditAction.CONVERSATION_CREATED, "CONVERSATION",
        saved.id(), "SUCCESS", "Channel: " + saved.channel());
    return toResponse(saved);
  }

  @Transactional(readOnly = true)
  public ConversationResponse findById(UUID id) {
    Conversation conversation = conversationRepository.findById(id);
    accessService.requirePermission(conversation.companyId(), CompanyPermission.CONVERSATION_READ);
    return toResponse(conversation);
  }

  @Transactional(readOnly = true)
  public PageResponse<ConversationResponse> search(String requestedCompanyId,
      ConversationStatus status, String externalParticipantId, int page, int size) {
    String companyId = accessService.resolveCompanyId(requestedCompanyId);
    accessService.requirePermission(companyId, CompanyPermission.CONVERSATION_READ);
    return conversationRepository.search(companyId, status, externalParticipantId,
        new PageQuery(page, size)).map(this::toResponse);
  }

  @Transactional
  public MessageResponse addMessage(UUID conversationId, CreateMessageRequest request) {
    Conversation conversation = conversationRepository.findByIdForUpdate(conversationId);
    accessService.requirePermission(conversation.companyId(), CompanyPermission.CONVERSATION_MANAGE);
    Instant now = Instant.now(clock);
    Message message = Message.create(conversationId, request.direction(), request.type(),
        request.content(), request.mediaUrl(), request.externalMessageId(), request.status(), now);
    Message saved = messageRepository.save(message);
    conversationRepository.save(conversation.touch(now));
    auditTrailService.record(conversation.companyId(), AuditAction.MESSAGE_ADDED, "CONVERSATION",
        conversationId, "SUCCESS", request.direction() + " " + request.type());
    return toResponse(saved);
  }

  @Transactional(readOnly = true)
  public PageResponse<MessageResponse> findMessages(
      UUID conversationId, int page, int size) {
    Conversation conversation = conversationRepository.findById(conversationId);
    accessService.requirePermission(conversation.companyId(), CompanyPermission.CONVERSATION_READ);
    return messageRepository.findByConversationId(conversationId, new PageQuery(page, size))
        .map(this::toResponse);
  }

  @Transactional
  public ConversationResponse close(UUID id) {
    Conversation conversation = conversationRepository.findByIdForUpdate(id);
    accessService.requirePermission(conversation.companyId(), CompanyPermission.CONVERSATION_MANAGE);
    Conversation saved = conversationRepository.save(conversation.close(Instant.now(clock)));
    auditTrailService.record(conversation.companyId(), AuditAction.CONVERSATION_CLOSED,
        "CONVERSATION", id, "SUCCESS", "Conversation closed");
    return toResponse(saved);
  }

  private void validateReferences(String companyId, String customerId, UUID draftId) {
    if (customerId != null && !customerId.isBlank()) {
      customerRepository.findByIdAndCompanyId(customerId, companyId);
    }
    if (draftId != null) {
      var draft = invoiceDraftRepository.findById(draftId);
      if (!draft.companyId().equals(companyId)
          || (customerId != null && !customerId.isBlank()
              && !draft.customerId().equals(customerId))) {
        throw new ApplicationException(CONVERSATION_RESOURCE_COMPANY_MISMATCH);
      }
    }
  }

  private ConversationResponse toResponse(Conversation value) {
    return new ConversationResponse(value.id(), value.companyId(), value.customerId(),
        value.invoiceDraftId(), value.channel(), value.externalParticipantId(), value.status(),
        value.createdAt(), value.updatedAt(), value.closedAt());
  }

  private MessageResponse toResponse(Message value) {
    return new MessageResponse(value.id(), value.conversationId(), value.direction(), value.type(),
        value.content(), value.mediaUrl(), value.externalMessageId(), value.status(), value.createdAt());
  }
}
