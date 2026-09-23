package com.invoiceautomationservice.application.service;

import com.invoiceautomationservice.application.dto.response.ElectronicDocumentResponse;
import com.invoiceautomationservice.application.dto.response.InvoiceItemResponse;
import com.invoiceautomationservice.application.port.out.ElectronicDocumentRepository;
import com.invoiceautomationservice.application.port.out.BillingProvider;
import com.invoiceautomationservice.domain.model.ElectronicDocument;
import com.invoiceautomationservice.domain.model.ElectronicDocumentStatus;
import com.invoiceautomationservice.domain.model.InvoiceItem;
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

  public ElectronicDocumentResponse toResponse(ElectronicDocument document) {
    return new ElectronicDocumentResponse(
        document.id(), document.draftId(), document.companyId(), document.customerId(),
        document.issuer(), document.recipient(),
        document.documentType(), document.series(), document.correlative(), document.fullNumber(),
        document.recipientDocumentType(), document.recipientDocumentNumber(), document.currency(),
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
