package com.invoiceautomationservice.application.service;

import static com.invoiceautomationservice.infrastructure.config.exception.RuntimeErrors.COMPANY_INACTIVE;
import static com.invoiceautomationservice.infrastructure.config.exception.RuntimeErrors.CUSTOMER_INACTIVE;
import static com.invoiceautomationservice.infrastructure.config.exception.RuntimeErrors.ISSUER_ONBOARDING_REQUIRED;
import static com.invoiceautomationservice.infrastructure.config.exception.RuntimeErrors.INVOICE_REQUIRES_RUC;
import static com.invoiceautomationservice.infrastructure.config.exception.RuntimeErrors.INVOICE_DRAFT_ALREADY_NUMBERED;

import com.invoiceautomationservice.application.dto.request.CreateInvoiceDraftRequest;
import com.invoiceautomationservice.application.dto.request.CreateInvoiceItemRequest;
import com.invoiceautomationservice.application.dto.request.UpdateInvoiceDraftRequest;
import com.invoiceautomationservice.application.dto.response.InvoiceDraftResponse;
import com.invoiceautomationservice.application.port.in.InvoiceDraftUseCase;
import com.invoiceautomationservice.application.port.out.CompanyRepository;
import com.invoiceautomationservice.application.port.out.IssuerTaxProfileRepository;
import com.invoiceautomationservice.application.port.out.InvoiceDraftRepository;
import com.invoiceautomationservice.application.port.out.ElectronicDocumentRepository;
import com.invoiceautomationservice.application.service.mapper.InvoiceDraftDomainResponseMapper;
import com.invoiceautomationservice.domain.model.Company;
import com.invoiceautomationservice.domain.model.Customer;
import com.invoiceautomationservice.domain.model.InvoiceDraft;
import com.invoiceautomationservice.domain.model.InvoiceItem;
import com.invoiceautomationservice.application.dto.response.ElectronicDocumentResponse;
import com.invoiceautomationservice.domain.model.IdentityDocumentType;
import com.invoiceautomationservice.domain.model.InvoiceDocumentType;
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
  private final InvoiceDraftDomainResponseMapper responseMapper;
  private final CompanyAccessService companyAccessService;
  private final IssuerTaxProfileRepository issuerTaxProfileRepository;
  private final RecipientResolutionService recipientResolutionService;
  private final ElectronicDocumentService electronicDocumentService;
  private final InvoiceIssuancePersistenceService issuancePersistenceService;
  private final BillingSubmissionService billingSubmissionService;
  private final ElectronicDocumentRepository electronicDocumentRepository;
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
  public InvoiceDraftResponse update(UUID id, UpdateInvoiceDraftRequest request) {
    InvoiceDraft draft = invoiceDraftRepository.findByIdForUpdate(id);
    companyAccessService.requireAccess(draft.companyId());
    draft.ensureEditable();
    if (request.documentType() == InvoiceDocumentType.INVOICE
        && request.recipientDocumentType() != IdentityDocumentType.RUC) {
      throw new ApplicationException(INVOICE_REQUIRES_RUC);
    }
    Customer customer = recipientResolutionService.resolve(
        draft.companyId(), request.recipientDocumentType(), request.recipientDocumentNumber());
    if (!customer.isActive()) {
      throw new ApplicationException(CUSTOMER_INACTIVE, customer.getId());
    }
    InvoiceDraft updated = draft.update(
        customer.getId(), request.documentType(), request.recipientDocumentType(),
        request.recipientDocumentNumber(), request.currency(), toItems(request.items()),
        Instant.now(clock));
    return responseMapper.toResponse(invoiceDraftRepository.save(updated));
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
  public InvoiceDraftResponse cancel(UUID id) {
    InvoiceDraft draft = invoiceDraftRepository.findByIdForUpdate(id);
    companyAccessService.requireAccess(draft.companyId());
    if (electronicDocumentRepository.findByDraftId(id).isPresent()) {
      throw new ApplicationException(INVOICE_DRAFT_ALREADY_NUMBERED, id);
    }
    InvoiceDraft cancelled = draft.cancel(Instant.now(clock));
    return responseMapper.toResponse(invoiceDraftRepository.save(cancelled));
  }

  @Override
  public ElectronicDocumentResponse issue(UUID id) {
    PreparedEmission prepared = issuancePersistenceService.prepare(id);
    if (!prepared.submitRequired()) {
      return electronicDocumentService.toResponse(prepared.document());
    }

    var completed = billingSubmissionService.submit(prepared.document());
    return electronicDocumentService.toResponse(completed);
  }

  private List<InvoiceItem> toItems(List<CreateInvoiceItemRequest> items) {
    return items.stream()
        .map(item -> InvoiceItem.create(
            item.description(), item.unitCode(), item.quantity(), item.unitPrice(),
            item.discount(), item.taxAffectation()))
        .toList();
  }
}
