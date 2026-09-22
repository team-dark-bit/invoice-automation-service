package com.invoiceautomationservice.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.invoiceautomationservice.application.dto.request.CreateInvoiceDraftRequest;
import com.invoiceautomationservice.application.dto.request.CreateInvoiceItemRequest;
import com.invoiceautomationservice.application.dto.response.InvoiceDraftResponse;
import com.invoiceautomationservice.application.dto.response.InvoiceItemResponse;
import com.invoiceautomationservice.application.dto.response.ElectronicDocumentResponse;
import com.invoiceautomationservice.application.port.out.CompanyRepository;
import com.invoiceautomationservice.application.port.out.BillingProvider;
import com.invoiceautomationservice.application.port.out.IssuerTaxProfileRepository;
import com.invoiceautomationservice.application.port.out.InvoiceDraftRepository;
import com.invoiceautomationservice.application.service.mapper.InvoiceDraftDomainResponseMapper;
import com.invoiceautomationservice.domain.model.Company;
import com.invoiceautomationservice.domain.model.BillingResult;
import com.invoiceautomationservice.domain.model.BillingSubmission;
import com.invoiceautomationservice.domain.model.ElectronicDocumentStatus;
import com.invoiceautomationservice.domain.model.Customer;
import com.invoiceautomationservice.domain.model.InvoiceDraft;
import com.invoiceautomationservice.domain.model.InvoiceItem;
import com.invoiceautomationservice.domain.model.IdentityDocumentType;
import com.invoiceautomationservice.domain.model.InvoiceDocumentType;
import com.invoiceautomationservice.domain.model.DocumentNumber;
import com.invoiceautomationservice.domain.model.ElectronicDocument;
import com.invoiceautomationservice.domain.model.IssuerSnapshot;
import com.invoiceautomationservice.domain.model.RecipientSnapshot;
import com.invoiceautomationservice.domain.model.TaxpayerType;
import com.invoiceautomationservice.infrastructure.config.exception.ApplicationException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class CreateInvoiceDraftServiceTest {

  private InvoiceDraftRepository draftRepository;
  private CompanyRepository companyRepository;
  private IssuerTaxProfileRepository issuerTaxProfileRepository;
  private RecipientResolutionService recipientResolutionService;
  private InvoiceDraftDomainResponseMapper mapper;
  private BillingProvider billingProvider;
  private InvoiceDraftService service;
  private CompanyAccessService accessService;
  private ElectronicDocumentService electronicDocumentService;
  private InvoiceIssuancePersistenceService issuancePersistenceService;

  @BeforeEach
  void setUp() {
    draftRepository = mock(InvoiceDraftRepository.class);
    companyRepository = mock(CompanyRepository.class);
    issuerTaxProfileRepository = mock(IssuerTaxProfileRepository.class);
    recipientResolutionService = mock(RecipientResolutionService.class);
    mapper = mock(InvoiceDraftDomainResponseMapper.class);
    billingProvider = mock(BillingProvider.class);
    accessService = mock(CompanyAccessService.class);
    electronicDocumentService = mock(ElectronicDocumentService.class);
    issuancePersistenceService = mock(InvoiceIssuancePersistenceService.class);
    Clock clock = Clock.fixed(Instant.parse("2026-09-01T10:00:00Z"), ZoneOffset.UTC);
    service = new InvoiceDraftService(
            draftRepository, companyRepository, billingProvider, mapper, accessService,
            issuerTaxProfileRepository, recipientResolutionService, electronicDocumentService,
            issuancePersistenceService, clock
    );
  }

  @Test
  void createsCompleteDraftAndCalculatesTotals() {
    CreateInvoiceDraftRequest request = request();
    when(companyRepository.findById("company-1")).thenReturn(company(true));
    when(issuerTaxProfileRepository.existsByCompanyId("company-1")).thenReturn(true);
    when(recipientResolutionService.resolve("company-1", IdentityDocumentType.DNI, "12345678"))
            .thenReturn(customer(true));
    when(draftRepository.save(any(InvoiceDraft.class))).thenAnswer(invocation -> invocation.getArgument(0));
    when(mapper.toResponse(any(InvoiceDraft.class))).thenAnswer(invocation -> response(invocation.getArgument(0)));

    InvoiceDraftResponse result = service.create(request);

    assertThat(result.id()).isNotNull();
    assertThat(result.companyId()).isEqualTo("company-1");
    assertThat(result.customerId()).isEqualTo("customer-1");
    assertThat(result.documentType()).isEqualTo(InvoiceDocumentType.SALES_RECEIPT);
    assertThat(result.currency()).isEqualTo("PEN");
    assertThat(result.items()).hasSize(2);
    assertThat(result.subtotal()).isEqualByComparingTo("350.50");
    assertThat(result.total()).isEqualByComparingTo("350.50");
    assertThat(result.createdAt()).isEqualTo(Instant.parse("2026-09-01T10:00:00Z"));
    verify(draftRepository).save(any(InvoiceDraft.class));
    verify(accessService).requireAccess("company-1");
  }

  @Test
  void rejectsInactiveCompany() {
    when(companyRepository.findById("company-1")).thenReturn(company(false));

    assertThatThrownBy(() -> service.create(request()))
            .isInstanceOf(ApplicationException.class)
            .hasMessage("The company with id: company-1 is inactive");
  }

  @Test
  void rejectsInactiveCustomer() {
    when(companyRepository.findById("company-1")).thenReturn(company(true));
    when(issuerTaxProfileRepository.existsByCompanyId("company-1")).thenReturn(true);
    when(recipientResolutionService.resolve("company-1", IdentityDocumentType.DNI, "12345678"))
            .thenReturn(customer(false));

    assertThatThrownBy(() -> service.create(request()))
            .isInstanceOf(ApplicationException.class)
            .hasMessage("The customer with id: customer-1 is inactive");
  }

  @Test
  void findsCompleteDraft() {
    InvoiceDraft draft = draft();
    InvoiceDraftResponse response = response(draft);
    when(draftRepository.findById(draft.id())).thenReturn(draft);
    when(mapper.toResponse(draft)).thenReturn(response);

    assertThat(service.findById(draft.id())).isSameAs(response);
    verify(accessService).requireAccess("company-1");
  }

  @Test
  void approvesDraftAndPersistsTransition() {
    InvoiceDraft draft = draft();
    when(draftRepository.findById(draft.id())).thenReturn(draft);
    when(draftRepository.save(any(InvoiceDraft.class))).thenAnswer(invocation -> invocation.getArgument(0));
    when(mapper.toResponse(any(InvoiceDraft.class))).thenAnswer(invocation -> response(invocation.getArgument(0)));

    InvoiceDraftResponse result = service.approve(draft.id());

    assertThat(result.status()).isEqualTo(com.invoiceautomationservice.domain.model.InvoiceDraftStatus.APPROVED);
    verify(draftRepository).save(any(InvoiceDraft.class));
  }

  @Test
  void issuesApprovedDraftThroughBillingProvider() {
    InvoiceDraft approved = draft().approve(Instant.parse("2026-09-01T09:00:00Z"));
    BillingResult billingResult = new BillingResult(
        "MOCK-" + approved.id(), ElectronicDocumentStatus.ACCEPTED,
        Instant.parse("2026-09-01T10:00:00Z"), Instant.parse("2026-09-01T10:00:00Z"),
        "0", "Accepted");
    ElectronicDocument pending = electronicDocument(approved);
    ElectronicDocument completed = pending.withBillingResult(billingResult);
    when(issuancePersistenceService.prepare(approved.id()))
        .thenReturn(new PreparedEmission(pending, true));
    when(billingProvider.submit(any(BillingSubmission.class))).thenReturn(billingResult);
    when(issuancePersistenceService.complete(pending.id(), billingResult)).thenReturn(completed);
    ElectronicDocumentResponse documentResponse = mock(ElectronicDocumentResponse.class);
    when(electronicDocumentService.toResponse(any(ElectronicDocument.class))).thenReturn(documentResponse);

    ElectronicDocumentResponse result = service.issue(approved.id());

    assertThat(result).isSameAs(documentResponse);
    ArgumentCaptor<BillingSubmission> submissionCaptor =
        ArgumentCaptor.forClass(BillingSubmission.class);
    verify(billingProvider).submit(submissionCaptor.capture());
    assertThat(submissionCaptor.getValue().issuer().taxId()).isEqualTo("20123456789");
    assertThat(submissionCaptor.getValue().recipient().name()).isEqualTo("Customer Name");
    assertThat(submissionCaptor.getValue().recipient().address()).isEqualTo("Customer address");
    assertThat(submissionCaptor.getValue().idempotencyKey())
        .isEqualTo(submissionCaptor.getValue().documentId().toString());
    verify(issuancePersistenceService).complete(pending.id(), billingResult);
  }

  @Test
  void doesNotCallBillingProviderWhenDraftIsNotApproved() {
    InvoiceDraft draft = draft();
    when(issuancePersistenceService.prepare(draft.id())).thenThrow(
        new com.invoiceautomationservice.domain.exception.InvalidInvoiceDraftStateException(
            draft.status(), com.invoiceautomationservice.domain.model.InvoiceDraftStatus.APPROVED));

    assertThatThrownBy(() -> service.issue(draft.id()))
            .isInstanceOf(com.invoiceautomationservice.domain.exception.InvalidInvoiceDraftStateException.class);
    verifyNoInteractions(billingProvider);
  }

  @Test
  void returnsExistingDocumentWithoutCallingProviderAgain() {
    InvoiceDraft draft = draft().approve(Instant.parse("2026-09-01T09:00:00Z"));
    ElectronicDocument existing = electronicDocument(draft);
    ElectronicDocumentResponse response = mock(ElectronicDocumentResponse.class);
    when(issuancePersistenceService.prepare(draft.id()))
        .thenReturn(new PreparedEmission(existing, false));
    when(electronicDocumentService.toResponse(existing)).thenReturn(response);

    ElectronicDocumentResponse result = service.issue(draft.id());

    assertThat(result).isSameAs(response);
    verifyNoInteractions(billingProvider);
  }

  @Test
  void persistsErrorWhenProviderCallFails() {
    InvoiceDraft approved = draft().approve(Instant.parse("2026-09-01T09:00:00Z"));
    ElectronicDocument pending = electronicDocument(approved);
    ElectronicDocumentResponse response = mock(ElectronicDocumentResponse.class);
    when(issuancePersistenceService.prepare(approved.id()))
        .thenReturn(new PreparedEmission(pending, true));
    when(billingProvider.submit(any(BillingSubmission.class)))
        .thenThrow(new IllegalStateException("Provider unavailable"));
    when(issuancePersistenceService.complete(any(UUID.class), any(BillingResult.class)))
        .thenAnswer(invocation -> pending.withBillingResult(invocation.getArgument(1)));
    when(electronicDocumentService.toResponse(any(ElectronicDocument.class))).thenReturn(response);

    assertThat(service.issue(approved.id())).isSameAs(response);

    ArgumentCaptor<BillingResult> resultCaptor = ArgumentCaptor.forClass(BillingResult.class);
    verify(issuancePersistenceService).complete(org.mockito.ArgumentMatchers.eq(pending.id()),
        resultCaptor.capture());
    assertThat(resultCaptor.getValue().status()).isEqualTo(ElectronicDocumentStatus.ERROR);
    assertThat(resultCaptor.getValue().responseCode()).isEqualTo("PROVIDER_CALL_FAILED");
    assertThat(resultCaptor.getValue().responseMessage()).isEqualTo("Provider unavailable");
  }

  private CreateInvoiceDraftRequest request() {
    return new CreateInvoiceDraftRequest(
        "company-1", InvoiceDocumentType.SALES_RECEIPT, IdentityDocumentType.DNI,
        "12345678", "PEN", List.of(
            new CreateInvoiceItemRequest("Consulting", new BigDecimal("2"), new BigDecimal("150.25")),
            new CreateInvoiceItemRequest("Support", BigDecimal.ONE, new BigDecimal("50.00"))
    ));
  }

  private InvoiceDraft draft() {
    return InvoiceDraft.create(
        "company-1", "customer-1", InvoiceDocumentType.SALES_RECEIPT,
        IdentityDocumentType.DNI, "12345678", "PEN", List.of(
            InvoiceItem.create("Consulting", new BigDecimal("2"), new BigDecimal("150.25")),
            InvoiceItem.create("Support", BigDecimal.ONE, new BigDecimal("50.00"))
    ), Instant.parse("2026-09-01T10:00:00Z"));
  }

  private Company company(boolean active) {
    Company company = new Company();
    company.setId("company-1");
    company.setTaxId("20123456789");
    company.setLegalName("Company SAC");
    company.setTradeName("Company");
    company.setActive(active);
    return company;
  }

  private Customer customer(boolean active) {
    Customer customer = new Customer();
    customer.setId("customer-1");
    customer.setCompanyId("company-1");
    customer.setFullName("Customer Name");
    customer.setAddress("Customer address");
    customer.setEmail("customer@test.pe");
    customer.setActive(active);
    return customer;
  }

  private ElectronicDocument electronicDocument(InvoiceDraft draft) {
    IssuerSnapshot issuer = new IssuerSnapshot(
        "20123456789", "Company SAC", "Company", TaxpayerType.LEGAL_ENTITY,
        "Lima", "150101", "Lima", "Lima", "Lima", "PE");
    RecipientSnapshot recipient = new RecipientSnapshot(
        IdentityDocumentType.DNI, "12345678", "Customer Name",
        "Customer address", "customer@test.pe");
    return ElectronicDocument.from(draft, new DocumentNumber("B001", 1), issuer, recipient);
  }

  private InvoiceDraftResponse response(InvoiceDraft draft) {
    List<InvoiceItemResponse> items = draft.items().stream().map(item -> new InvoiceItemResponse(
            item.id(), item.description(), item.quantity(), item.unitPrice(), item.lineTotal()
    )).toList();
    return new InvoiceDraftResponse(
            draft.id(), draft.companyId(), draft.customerId(), draft.documentType(),
            draft.recipientDocumentType(), draft.recipientDocumentNumber(),
            draft.currency(), draft.status(),
            items, draft.subtotal(), draft.total(), draft.createdAt(), draft.updatedAt(),
            draft.providerReference(), draft.issuedAt()
    );
  }
}
