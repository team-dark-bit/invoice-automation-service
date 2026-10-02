package com.invoiceautomationservice.application.service;

import com.invoiceautomationservice.application.model.ImageProcessingWork;
import com.invoiceautomationservice.application.port.out.ConversationContextRepository;
import com.invoiceautomationservice.application.port.out.ConversationImageRepository;
import com.invoiceautomationservice.application.port.out.ConversationRepository;
import com.invoiceautomationservice.domain.model.AuditAction;
import com.invoiceautomationservice.domain.model.ConversationContext;
import com.invoiceautomationservice.domain.model.ConversationImage;
import com.invoiceautomationservice.domain.model.DocumentInterpretation;
import com.invoiceautomationservice.domain.model.ImageProcessingStatus;
import com.invoiceautomationservice.domain.model.InterpretationContextSnapshot;
import com.invoiceautomationservice.domain.model.InterpretationSource;
import com.invoiceautomationservice.domain.model.InterpretedInvoiceItem;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ImageProcessingStateService {
  private final ConversationImageRepository imageRepository;
  private final ConversationRepository conversationRepository;
  private final ConversationContextRepository contextRepository;
  private final AuditTrailService auditTrailService;
  private final Clock clock;

  @Transactional
  public Optional<ImageProcessingWork> start(UUID imageId) {
    ConversationImage image = imageRepository.findByIdForUpdate(imageId).orElse(null);
    if (image == null || !image.active()
        || image.processingStatus() != ImageProcessingStatus.RECEIVED) {
      return Optional.empty();
    }
    Instant now = Instant.now(clock);
    ConversationImage processing = imageRepository.save(image.startProcessing(now));
    var conversation = conversationRepository.findById(image.conversationId());
    InterpretationContextSnapshot context = contextRepository
        .findByConversationId(image.conversationId())
        .map(this::toSnapshot)
        .orElseGet(InterpretationContextSnapshot::empty);
    auditTrailService.recordAs("system", conversation.companyId(),
        AuditAction.IMAGE_PROCESSING_STARTED, "CONVERSATION_IMAGE", image.id(), "SUCCESS",
        "Attempt " + processing.processingAttempts());
    return Optional.of(new ImageProcessingWork(processing, conversation.companyId(), context));
  }

  @Transactional
  public void complete(UUID imageId, DocumentInterpretation interpretation) {
    ConversationImage image = imageRepository.findByIdForUpdate(imageId).orElse(null);
    if (image == null || image.processingStatus() != ImageProcessingStatus.PROCESSING) return;
    ConversationImage completed = imageRepository.save(
        image.extracted(interpretation, Instant.now(clock)));
    String companyId = conversationRepository.findByIdForUpdate(image.conversationId()).companyId();
    ConversationContext context = contextRepository.findByConversationId(image.conversationId())
        .orElseGet(() -> ConversationContext.empty(image.conversationId(), Instant.now(clock)));
    contextRepository.save(context.stageInterpretation(
        interpretation, com.invoiceautomationservice.domain.model.ConversationFlowState.NEEDS_REVIEW,
        Instant.now(clock)));
    auditTrailService.recordAs("system", companyId, AuditAction.IMAGE_EXTRACTED,
        "CONVERSATION_IMAGE", image.id(), "SUCCESS",
        "Intent: " + completed.interpretation().intent()
            + ", confidence: " + completed.interpretation().confidence());
  }

  @Transactional
  public void fail(UUID imageId, String error) {
    ConversationImage image = imageRepository.findByIdForUpdate(imageId).orElse(null);
    if (image == null || image.processingStatus() != ImageProcessingStatus.PROCESSING) return;
    String safeError = error == null || error.isBlank() ? "Multimodal processing failed" : error;
    if (safeError.length() > 1000) safeError = safeError.substring(0, 1000);
    imageRepository.save(image.failProcessing(safeError, Instant.now(clock)));
    String companyId = conversationRepository.findByIdForUpdate(image.conversationId()).companyId();
    ConversationContext context = contextRepository.findByConversationId(image.conversationId())
        .orElseGet(() -> ConversationContext.empty(image.conversationId(), Instant.now(clock)));
    contextRepository.save(context.markMediaFailed(DocumentInterpretation.unsupported(
        InterpretationSource.IMAGE, safeError), Instant.now(clock)));
    auditTrailService.recordAs("system", companyId, AuditAction.IMAGE_PROCESSING_FAILED,
        "CONVERSATION_IMAGE", image.id(), "FAILED", safeError);
  }

  private InterpretationContextSnapshot toSnapshot(ConversationContext context) {
    List<InterpretedInvoiceItem> items = context.items().stream().map(item ->
        new InterpretedInvoiceItem(item.description(), item.unitCode(), item.quantity(),
            item.unitPrice(), item.discount(), item.taxAffectation(),
            item.quantity().multiply(item.unitPrice()), BigDecimal.ONE, List.of())).toList();
    return new InterpretationContextSnapshot(context.documentType(),
        context.recipientDocumentType(), context.recipientDocumentNumber(), context.currency(),
        items);
  }
}
