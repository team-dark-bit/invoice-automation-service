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
import com.invoiceautomationservice.application.port.out.CustomerRepository;
import com.invoiceautomationservice.application.port.out.IssuerTaxProfileRepository;
import com.invoiceautomationservice.application.port.out.InvoiceDraftRepository;
import com.invoiceautomationservice.application.port.out.DocumentSeriesRepository;
import com.invoiceautomationservice.application.port.out.ElectronicDocumentRepository;
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
import com.invoiceautomationservice.domain.model.IssuerTaxProfile;
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
  private final CustomerRepository customerRepository;
  private final BillingProvider billingProvider;
  private final InvoiceDraftDomainResponseMapper responseMapper;
  private final CompanyAccessService companyAccessService;
  private final IssuerTaxProfileRepository issuerTaxProfileRepository;
  private final RecipientResolutionService recipientResolutionService;
  private final DocumentSeriesRepository documentSeriesRepository;
  private final ElectronicDocumentRepository electronicDocumentRepository;
  private final ElectronicDocumentService electronicDocumentService;
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
  @Transactional
  public ElectronicDocumentResponse issue(UUID id) {
    InvoiceDraft approved = invoiceDraftRepository.findByIdForUpdate(id);
    companyAccessService.requireAccess(approved.companyId());
    var existingDocument = electronicDocumentRepository.findByDraftId(id);
    if (existingDocument.isPresent()) {
      return electronicDocumentService.toResponse(existingDocument.get());
    }
    approved.ensureCanBeIssued();
    var documentNumber = documentSeriesRepository.reserveNext(
        approved.companyId(), approved.documentType());
    Company company = companyRepository.findById(approved.companyId());
    Customer customer = customerRepository.findByIdAndCompanyId(
        approved.customerId(), approved.companyId());
    IssuerTaxProfile taxProfile = issuerTaxProfileRepository.findByCompanyId(approved.companyId());
    IssuerSnapshot issuer = toIssuerSnapshot(company, taxProfile);
    RecipientSnapshot recipient = toRecipientSnapshot(approved, customer);
    ElectronicDocument provisional = ElectronicDocument.from(
        approved, documentNumber, issuer, recipient);
    electronicDocumentRepository.save(provisional);
    BillingResult result = billingProvider.submit(toBillingSubmission(provisional));
    ElectronicDocument issuedDocument = provisional.withBillingResult(result);
    ElectronicDocument savedDocument = electronicDocumentRepository.save(issuedDocument);
    if (result.status() == ElectronicDocumentStatus.SENT
        || result.status() == ElectronicDocumentStatus.ACCEPTED) {
      InvoiceDraft issued = approved.markIssued(result.reference(), result.submittedAt());
      invoiceDraftRepository.save(issued);
    }
    return electronicDocumentService.toResponse(savedDocument);
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
