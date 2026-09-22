package com.invoiceautomationservice.application.service;

import static com.invoiceautomationservice.infrastructure.config.exception.RuntimeErrors.COMPANY_INACTIVE;
import static com.invoiceautomationservice.infrastructure.config.exception.RuntimeErrors.CUSTOMER_INACTIVE;
import static com.invoiceautomationservice.infrastructure.config.exception.RuntimeErrors.ISSUER_ONBOARDING_REQUIRED;
import static com.invoiceautomationservice.infrastructure.config.exception.RuntimeErrors.INVOICE_REQUIRES_RUC;

import com.invoiceautomationservice.application.dto.request.CreateInvoiceDraftRequest;
import com.invoiceautomationservice.application.dto.response.InvoiceDraftResponse;
import com.invoiceautomationservice.application.port.in.InvoiceDraftUseCase;
import com.invoiceautomationservice.application.port.out.CompanyRepository;
import com.invoiceautomationservice.application.port.out.BillingProvider;
import com.invoiceautomationservice.application.port.out.IssuerTaxProfileRepository;
import com.invoiceautomationservice.application.port.out.InvoiceDraftRepository;
import com.invoiceautomationservice.application.service.mapper.InvoiceDraftDomainResponseMapper;
import com.invoiceautomationservice.domain.model.Company;
import com.invoiceautomationservice.domain.model.BillingResult;
import com.invoiceautomationservice.domain.model.BillingSubmission;
import com.invoiceautomationservice.domain.model.Customer;
import com.invoiceautomationservice.domain.model.InvoiceDraft;
import com.invoiceautomationservice.domain.model.InvoiceItem;
import com.invoiceautomationservice.domain.model.ElectronicDocument;
import com.invoiceautomationservice.domain.model.ElectronicDocumentStatus;
import com.invoiceautomationservice.application.dto.response.ElectronicDocumentResponse;
import com.invoiceautomationservice.domain.model.IdentityDocumentType;
import com.invoiceautomationservice.domain.model.InvoiceDocumentType;
import com.invoiceautomationservice.domain.model.IssuerSnapshot;
import com.invoiceautomationservice.domain.model.RecipientSnapshot;
import com.invoiceautomationservice.infrastructure.config.exception.ApplicationException;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InvoiceDraftService implements InvoiceDraftUseCase {

  private final InvoiceDraftRepository invoiceDraftRepository;
  private final CompanyRepository companyRepository;
  private final BillingProvider billingProvider;
  private final InvoiceDraftDomainResponseMapper responseMapper;
  private final CompanyAccessService companyAccessService;
  private final IssuerTaxProfileRepository issuerTaxProfileRepository;
  private final RecipientResolutionService recipientResolutionService;
  private final ElectronicDocumentService electronicDocumentService;
  private final InvoiceIssuancePersistenceService issuancePersistenceService;
  private final Clock clock;

  @Override
  @Transactional
  public InvoiceDraftResponse create(CreateInvoiceDraftRequest request) {
    companyAccessService.requireAccess(request.companyId());
    Company company = companyRepository.findById(request.companyId());
    if (!company.isActive()) {
      throw new ApplicationException(COMPANY_INACTIVE, request.companyId());
    }
    if (!issuerTaxProfileRepository.existsByCompanyId(request.companyId())) {
      throw new ApplicationException(ISSUER_ONBOARDING_REQUIRED, request.companyId());
    }
    if (request.documentType() == InvoiceDocumentType.INVOICE
        && request.recipientDocumentType() != IdentityDocumentType.RUC) {
      throw new ApplicationException(INVOICE_REQUIRES_RUC);
    }
    Customer customer = recipientResolutionService.resolve(
        request.companyId(), request.recipientDocumentType(), request.recipientDocumentNumber());
    if (!customer.isActive()) {
      throw new ApplicationException(CUSTOMER_INACTIVE, customer.getId());
    }

    List<InvoiceItem> items = request.items().stream()
            .map(item -> InvoiceItem.create(
                item.description(), item.unitCode(), item.quantity(), item.unitPrice(),
                item.discount(), item.taxAffectation()))
            .toList();
    InvoiceDraft draft = InvoiceDraft.create(
            request.companyId(), customer.getId(), request.documentType(),
            request.recipientDocumentType(), request.recipientDocumentNumber(),
            request.currency(), items, Instant.now(clock)
    );
    return responseMapper.toResponse(invoiceDraftRepository.save(draft));
  }

  @Override
  @Transactional(readOnly = true)
  public InvoiceDraftResponse findById(UUID id) {
    InvoiceDraft draft = invoiceDraftRepository.findById(id);
    companyAccessService.requireAccess(draft.companyId());
    return responseMapper.toResponse(draft);
  }

  @Override
  @Transactional
  public InvoiceDraftResponse approve(UUID id) {
    InvoiceDraft draft = invoiceDraftRepository.findById(id);
    companyAccessService.requireAccess(draft.companyId());
    InvoiceDraft approved = draft.approve(Instant.now(clock));
    return responseMapper.toResponse(invoiceDraftRepository.save(approved));
  }

  @Override
  public ElectronicDocumentResponse issue(UUID id) {
    PreparedEmission prepared = issuancePersistenceService.prepare(id);
    if (!prepared.submitRequired()) {
      return electronicDocumentService.toResponse(prepared.document());
    }

    BillingResult result;
    try {
      result = billingProvider.submit(toBillingSubmission(prepared.document()));
    } catch (RuntimeException exception) {
      Instant failedAt = Instant.now(clock);
      result = new BillingResult(
          null, ElectronicDocumentStatus.ERROR,
          failedAt, failedAt, "PROVIDER_CALL_FAILED", providerFailureMessage(exception));
    }
    ElectronicDocument completed = issuancePersistenceService.complete(
        prepared.document().id(), result);
    return electronicDocumentService.toResponse(completed);
  }

  private String providerFailureMessage(RuntimeException exception) {
    String message = exception.getMessage();
    String detail = message == null || message.isBlank()
        ? exception.getClass().getSimpleName() : message;
    return detail.length() <= 1000 ? detail : detail.substring(0, 1000);
  }

  private BillingSubmission toBillingSubmission(ElectronicDocument document) {
    IssuerSnapshot snapshot = document.issuer();
    BillingSubmission.Issuer issuer = new BillingSubmission.Issuer(
        snapshot.taxId(), snapshot.legalName(), snapshot.tradeName(), snapshot.taxpayerType(),
        snapshot.fiscalAddress(), snapshot.ubigeo(), snapshot.department(), snapshot.province(),
        snapshot.district(), snapshot.countryCode());
    RecipientSnapshot recipientSnapshot = document.recipient();
    BillingSubmission.Recipient recipient = new BillingSubmission.Recipient(
        recipientSnapshot.documentType(), recipientSnapshot.documentNumber(),
        recipientSnapshot.name(), recipientSnapshot.address(), recipientSnapshot.email());
    List<BillingSubmission.Item> providerItems = document.items().stream()
        .map(item -> new BillingSubmission.Item(
            item.description(), item.unitCode(), item.quantity(), item.unitPrice(), item.discount(),
            item.taxAffectation(), item.taxRate(), item.grossAmount(), item.taxableAmount(),
            item.taxAmount(), item.lineTotal()))
        .toList();
    return new BillingSubmission(
        document.id(), document.id().toString(), document.fullNumber(),
        document.series(), document.correlative(),
        document.documentType(), document.currency(),
        issuer, recipient, providerItems, document.subtotal(), document.discountTotal(),
        document.taxableTotal(), document.taxTotal(), document.total());
  }
}
