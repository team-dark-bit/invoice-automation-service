package com.invoiceautomationservice.application.service;

import com.invoiceautomationservice.application.port.out.CompanyRepository;
import com.invoiceautomationservice.application.port.out.CustomerRepository;
import com.invoiceautomationservice.application.port.out.DocumentSeriesRepository;
import com.invoiceautomationservice.application.port.out.ElectronicDocumentRepository;
import com.invoiceautomationservice.application.port.out.InvoiceDraftRepository;
import com.invoiceautomationservice.application.port.out.IssuerTaxProfileRepository;
import com.invoiceautomationservice.domain.model.BillingResult;
import com.invoiceautomationservice.domain.model.Company;
import com.invoiceautomationservice.domain.model.Customer;
import com.invoiceautomationservice.domain.model.ElectronicDocument;
import com.invoiceautomationservice.domain.model.ElectronicDocumentStatus;
import com.invoiceautomationservice.domain.model.InvoiceDraft;
import com.invoiceautomationservice.domain.model.InvoiceDraftStatus;
import com.invoiceautomationservice.domain.model.IssuerSnapshot;
import com.invoiceautomationservice.domain.model.IssuerTaxProfile;
import com.invoiceautomationservice.domain.model.RecipientSnapshot;
import java.util.UUID;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InvoiceIssuancePersistenceService {

  private static final Duration STALE_SUBMISSION_TIMEOUT = Duration.ofMinutes(5);

  private final InvoiceDraftRepository invoiceDraftRepository;
  private final ElectronicDocumentRepository electronicDocumentRepository;
  private final DocumentSeriesRepository documentSeriesRepository;
  private final CompanyRepository companyRepository;
  private final CustomerRepository customerRepository;
  private final IssuerTaxProfileRepository issuerTaxProfileRepository;
  private final CompanyAccessService companyAccessService;
  private final Clock clock;

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public PreparedEmission prepare(UUID draftId) {
    InvoiceDraft draft = invoiceDraftRepository.findByIdForUpdate(draftId);
    companyAccessService.requireAccess(draft.companyId());
    var existing = electronicDocumentRepository.findByDraftId(draftId);
    if (existing.isPresent()) {
      return new PreparedEmission(existing.get(), false);
    }

    draft.ensureCanBeIssued();
    var number = documentSeriesRepository.reserveNext(draft.companyId(), draft.documentType());
    Company company = companyRepository.findById(draft.companyId());
    Customer customer = customerRepository.findByIdAndCompanyId(
        draft.customerId(), draft.companyId());
    IssuerTaxProfile profile = issuerTaxProfileRepository.findByCompanyId(draft.companyId());
    ElectronicDocument document = ElectronicDocument.from(
        draft, number, toIssuerSnapshot(company, profile), toRecipientSnapshot(draft, customer))
        .startSubmission(Instant.now(clock));
    return new PreparedEmission(electronicDocumentRepository.save(document), true);
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public PreparedEmission prepareRetry(UUID documentId) {
    ElectronicDocument document = electronicDocumentRepository.findByIdForUpdate(documentId);
    companyAccessService.requireAccess(document.companyId());
    Instant now = Instant.now(clock);
    boolean staleSubmission = document.status() == ElectronicDocumentStatus.SENDING
        && document.submittedAt() != null
        && !document.submittedAt().plus(STALE_SUBMISSION_TIMEOUT).isAfter(now);
    if (document.status() != ElectronicDocumentStatus.PENDING_SEND
        && document.status() != ElectronicDocumentStatus.ERROR
        && !staleSubmission) {
      throw new com.invoiceautomationservice.infrastructure.config.exception.ApplicationException(
          com.invoiceautomationservice.infrastructure.config.exception.RuntimeErrors
              .ELECTRONIC_DOCUMENT_NOT_RETRYABLE,
          document.id(), document.status());
    }
    return new PreparedEmission(
        electronicDocumentRepository.save(document.startSubmission(now)), true);
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public PreparedEmission prepareAdjustment(
      UUID originalId, com.invoiceautomationservice.domain.model.InvoiceDocumentType noteType,
      String reasonCode, String reason) {
    ElectronicDocument original = electronicDocumentRepository.findByIdForUpdate(originalId);
    companyAccessService.requireAccess(original.companyId());
    var existing = electronicDocumentRepository.findAdjustment(originalId, noteType, reasonCode);
    if (existing.isPresent()) return new PreparedEmission(existing.get(), false);
    if (original.status() != ElectronicDocumentStatus.ACCEPTED
        || !original.documentType().isPrimaryDocument()) {
      throw new com.invoiceautomationservice.infrastructure.config.exception.ApplicationException(
          com.invoiceautomationservice.infrastructure.config.exception.RuntimeErrors
              .ELECTRONIC_DOCUMENT_NOT_ADJUSTABLE, original.id(), original.status());
    }
    var number = documentSeriesRepository.reserveNext(
        original.companyId(), noteType, original.series().substring(0, 1));
    ElectronicDocument note = ElectronicDocument.noteFrom(
        original, number, noteType, reasonCode, reason, Instant.now(clock))
        .startSubmission(Instant.now(clock));
    return new PreparedEmission(electronicDocumentRepository.save(note), true);
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public ElectronicDocument complete(
      UUID documentId, Instant attemptStartedAt, BillingResult result) {
    ElectronicDocument document = electronicDocumentRepository.findByIdForUpdate(documentId);
    if (document.status() == ElectronicDocumentStatus.ACCEPTED
        || document.status() == ElectronicDocumentStatus.REJECTED) {
      return document;
    }
    if (document.status() != ElectronicDocumentStatus.SENDING
        || !attemptStartedAt.equals(document.submittedAt())) {
      return document;
    }
    ElectronicDocument saved = electronicDocumentRepository.save(
        document.withBillingResult(result));
    if (document.draftId() != null && (result.status() == ElectronicDocumentStatus.SENT
        || result.status() == ElectronicDocumentStatus.ACCEPTED)) {
      InvoiceDraft draft = invoiceDraftRepository.findByIdForUpdate(document.draftId());
      if (draft.status() == InvoiceDraftStatus.APPROVED) {
        invoiceDraftRepository.save(draft.markIssued(result.reference(), result.submittedAt()));
      }
    }
    return saved;
  }

  private IssuerSnapshot toIssuerSnapshot(Company company, IssuerTaxProfile profile) {
    return new IssuerSnapshot(
        company.getTaxId(), company.getLegalName(), company.getTradeName(),
        profile.taxpayerType(), profile.fiscalAddress(), profile.ubigeo(), profile.department(),
        profile.province(), profile.district(), profile.countryCode());
  }

  private RecipientSnapshot toRecipientSnapshot(InvoiceDraft draft, Customer customer) {
    String name = customer.getCompanyName() == null || customer.getCompanyName().isBlank()
        ? customer.getFullName() : customer.getCompanyName();
    return new RecipientSnapshot(
        draft.recipientDocumentType(), draft.recipientDocumentNumber(), name,
        customer.getAddress(), customer.getEmail());
  }
}
