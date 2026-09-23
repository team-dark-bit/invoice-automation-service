package com.invoiceautomationservice.application.service;

import com.invoiceautomationservice.application.dto.response.ElectronicDocumentResponse;
import com.invoiceautomationservice.application.dto.response.InvoiceItemResponse;
import com.invoiceautomationservice.application.dto.response.PageResponse;
import com.invoiceautomationservice.application.model.PageQuery;
import com.invoiceautomationservice.application.port.out.ElectronicDocumentRepository;
import com.invoiceautomationservice.application.port.out.BillingProvider;
import com.invoiceautomationservice.domain.model.ElectronicDocument;
import com.invoiceautomationservice.domain.model.ElectronicDocumentStatus;
import com.invoiceautomationservice.domain.model.InvoiceItem;
import com.invoiceautomationservice.domain.model.InvoiceDocumentType;
import com.invoiceautomationservice.domain.model.CreditNoteReason;
import com.invoiceautomationservice.domain.model.DebitNoteReason;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ElectronicDocumentService {

  private final ElectronicDocumentRepository repository;
  private final CompanyAccessService companyAccessService;
  private final BillingProvider billingProvider;
  private final InvoiceIssuancePersistenceService issuancePersistenceService;
  private final BillingSubmissionService billingSubmissionService;

  @Transactional(readOnly = true)
  public ElectronicDocumentResponse findById(UUID id) {
    ElectronicDocument document = repository.findById(id);
    companyAccessService.requireAccess(document.companyId());
    return toResponse(document);
  }

  @Transactional(readOnly = true)
  public PageResponse<ElectronicDocumentResponse> search(
      String requestedCompanyId, ElectronicDocumentStatus status, String documentNumber,
      int page, int size) {
    String companyId = companyAccessService.resolveCompanyId(requestedCompanyId);
    return repository.search(companyId, status, documentNumber, new PageQuery(page, size))
        .map(this::toResponse);
  }

  @Transactional
  public ElectronicDocumentResponse refreshStatus(UUID id) {
    ElectronicDocument document = repository.findById(id);
    companyAccessService.requireAccess(document.companyId());
    if (document.status() == ElectronicDocumentStatus.ACCEPTED
        || document.status() == ElectronicDocumentStatus.REJECTED) {
      return toResponse(document);
    }
    if (document.providerReference() == null || document.providerReference().isBlank()) {
      throw new IllegalStateException("document has no provider reference to query");
    }
    ElectronicDocument updated = document.withBillingResult(
        billingProvider.checkStatus(document.providerReference()));
    return toResponse(repository.save(updated));
  }

  public ElectronicDocumentResponse retry(UUID id) {
    PreparedEmission prepared = issuancePersistenceService.prepareRetry(id);
    ElectronicDocument completed = billingSubmissionService.submit(prepared.document());
    return toResponse(completed);
  }

  public ElectronicDocumentResponse createCreditNote(
      UUID originalId, String reasonCode, String reason) {
    try {
      CreditNoteReason.fromCode(reasonCode);
    } catch (IllegalArgumentException exception) {
      throw new com.invoiceautomationservice.infrastructure.config.exception.ApplicationException(
          com.invoiceautomationservice.infrastructure.config.exception.RuntimeErrors.INVALID_NOTE_REASON,
          reasonCode, InvoiceDocumentType.CREDIT_NOTE);
    }
    return createAdjustment(originalId, InvoiceDocumentType.CREDIT_NOTE, reasonCode, reason);
  }

  public ElectronicDocumentResponse createDebitNote(
      UUID originalId, String reasonCode, String reason) {
    try {
      DebitNoteReason.fromCode(reasonCode);
    } catch (IllegalArgumentException exception) {
      throw new com.invoiceautomationservice.infrastructure.config.exception.ApplicationException(
          com.invoiceautomationservice.infrastructure.config.exception.RuntimeErrors.INVALID_NOTE_REASON,
          reasonCode, InvoiceDocumentType.DEBIT_NOTE);
    }
    return createAdjustment(originalId, InvoiceDocumentType.DEBIT_NOTE, reasonCode, reason);
  }

  public ElectronicDocumentResponse cancel(UUID originalId, String reason) {
    return createAdjustment(originalId, InvoiceDocumentType.CREDIT_NOTE,
        CreditNoteReason.OPERATION_CANCELLATION.code(), reason);
  }

  private ElectronicDocumentResponse createAdjustment(
      UUID originalId, InvoiceDocumentType type, String reasonCode, String reason) {
    PreparedEmission prepared = issuancePersistenceService.prepareAdjustment(
        originalId, type, reasonCode, reason);
    ElectronicDocument result = prepared.submitRequired()
        ? billingSubmissionService.submit(prepared.document()) : prepared.document();
    return toResponse(result);
  }

  public ElectronicDocumentResponse toResponse(ElectronicDocument document) {
    return new ElectronicDocumentResponse(
        document.id(), document.draftId(), document.companyId(), document.customerId(),
        document.issuer(), document.recipient(),
        document.documentType(), document.relatedDocumentId(), document.relatedDocumentType(),
        document.relatedSeries(), document.relatedCorrelative(), document.noteReasonCode(),
        document.noteReason(), document.series(), document.correlative(), document.fullNumber(),
        document.recipientDocumentType(), document.recipientDocumentNumber(), document.currency(),
        document.emissionAt(),
        document.items().stream().map(this::toItemResponse).toList(), document.subtotal(),
        document.discountTotal(), document.taxableTotal(), document.taxTotal(), document.total(),
        document.status(), document.providerReference(), document.submittedAt(),
        document.respondedAt(), document.providerResponseCode(), document.providerResponseMessage());
  }

  private InvoiceItemResponse toItemResponse(InvoiceItem item) {
    return new InvoiceItemResponse(
        item.id(), item.description(), item.unitCode(), item.quantity(), item.unitPrice(),
        item.discount(), item.taxAffectation(), item.taxRate(), item.grossAmount(),
        item.taxableAmount(), item.taxAmount(), item.lineTotal());
  }
}
