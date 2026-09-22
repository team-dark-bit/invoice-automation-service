package com.invoiceautomationservice.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.invoiceautomationservice.application.port.out.CompanyRepository;
import com.invoiceautomationservice.application.port.out.CustomerRepository;
import com.invoiceautomationservice.application.port.out.DocumentSeriesRepository;
import com.invoiceautomationservice.application.port.out.ElectronicDocumentRepository;
import com.invoiceautomationservice.application.port.out.InvoiceDraftRepository;
import com.invoiceautomationservice.application.port.out.IssuerTaxProfileRepository;
import com.invoiceautomationservice.domain.model.BillingResult;
import com.invoiceautomationservice.domain.model.Company;
import com.invoiceautomationservice.domain.model.Customer;
import com.invoiceautomationservice.domain.model.DocumentNumber;
import com.invoiceautomationservice.domain.model.ElectronicDocument;
import com.invoiceautomationservice.domain.model.ElectronicDocumentStatus;
import com.invoiceautomationservice.domain.model.IdentityDocumentType;
import com.invoiceautomationservice.domain.model.InvoiceDocumentType;
import com.invoiceautomationservice.domain.model.InvoiceDraft;
import com.invoiceautomationservice.domain.model.InvoiceDraftStatus;
import com.invoiceautomationservice.domain.model.InvoiceItem;
import com.invoiceautomationservice.domain.model.IssuerSnapshot;
import com.invoiceautomationservice.domain.model.IssuerTaxProfile;
import com.invoiceautomationservice.domain.model.RecipientSnapshot;
import com.invoiceautomationservice.domain.model.TaxpayerType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InvoiceIssuancePersistenceServiceTest {

  private InvoiceDraftRepository draftRepository;
  private ElectronicDocumentRepository documentRepository;
  private DocumentSeriesRepository seriesRepository;
  private CompanyRepository companyRepository;
  private CustomerRepository customerRepository;
  private IssuerTaxProfileRepository profileRepository;
  private CompanyAccessService accessService;
  private InvoiceIssuancePersistenceService service;

  @BeforeEach
  void setUp() {
    draftRepository = mock(InvoiceDraftRepository.class);
    documentRepository = mock(ElectronicDocumentRepository.class);
    seriesRepository = mock(DocumentSeriesRepository.class);
    companyRepository = mock(CompanyRepository.class);
    customerRepository = mock(CustomerRepository.class);
    profileRepository = mock(IssuerTaxProfileRepository.class);
    accessService = mock(CompanyAccessService.class);
    service = new InvoiceIssuancePersistenceService(
        draftRepository, documentRepository, seriesRepository, companyRepository,
        customerRepository, profileRepository, accessService);
  }

  @Test
  void preparesAndPersistsPendingDocumentBeforeSubmission() {
    InvoiceDraft approved = approvedDraft();
    when(draftRepository.findByIdForUpdate(approved.id())).thenReturn(approved);
    when(documentRepository.findByDraftId(approved.id())).thenReturn(Optional.empty());
    when(seriesRepository.reserveNext("company-1", InvoiceDocumentType.SALES_RECEIPT))
        .thenReturn(new DocumentNumber("B001", 7));
    when(companyRepository.findById("company-1")).thenReturn(company());
    when(customerRepository.findByIdAndCompanyId("customer-1", "company-1"))
        .thenReturn(customer());
    when(profileRepository.findByCompanyId("company-1")).thenReturn(profile());
    when(documentRepository.save(any(ElectronicDocument.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    PreparedEmission result = service.prepare(approved.id());

    assertThat(result.submitRequired()).isTrue();
    assertThat(result.document().status()).isEqualTo(ElectronicDocumentStatus.PENDING_SEND);
    assertThat(result.document().fullNumber()).isEqualTo("B001-00000007");
    verify(accessService).requireAccess("company-1");
    verify(documentRepository).save(result.document());
  }

  @Test
  void returnsExistingDocumentWithoutReservingAnotherNumber() {
    InvoiceDraft approved = approvedDraft();
    ElectronicDocument existing = document(approved);
    when(draftRepository.findByIdForUpdate(approved.id())).thenReturn(approved);
    when(documentRepository.findByDraftId(approved.id())).thenReturn(Optional.of(existing));

    PreparedEmission result = service.prepare(approved.id());

    assertThat(result).isEqualTo(new PreparedEmission(existing, false));
    verifyNoInteractions(seriesRepository, companyRepository, customerRepository, profileRepository);
  }

  @Test
  void completesDocumentAndMarksDraftIssuedForAcceptedResult() {
    InvoiceDraft approved = approvedDraft();
    ElectronicDocument pending = document(approved);
    Instant submittedAt = Instant.parse("2026-09-01T10:00:00Z");
    BillingResult providerResult = new BillingResult(
        "provider-1", ElectronicDocumentStatus.ACCEPTED, submittedAt, submittedAt,
        "0", "Accepted");
    when(documentRepository.findByIdForUpdate(pending.id())).thenReturn(pending);
    when(documentRepository.save(any(ElectronicDocument.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(draftRepository.findByIdForUpdate(approved.id())).thenReturn(approved);
    when(draftRepository.save(any(InvoiceDraft.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    ElectronicDocument completed = service.complete(pending.id(), providerResult);

    assertThat(completed.status()).isEqualTo(ElectronicDocumentStatus.ACCEPTED);
    verify(draftRepository).save(org.mockito.ArgumentMatchers.argThat(
        draft -> draft.status() == InvoiceDraftStatus.ISSUED
            && "provider-1".equals(draft.providerReference())));
  }

  @Test
  void errorResultDoesNotMarkDraftIssued() {
    InvoiceDraft approved = approvedDraft();
    ElectronicDocument pending = document(approved);
    Instant failedAt = Instant.parse("2026-09-01T10:00:00Z");
    BillingResult error = new BillingResult(
        null, ElectronicDocumentStatus.ERROR, failedAt, failedAt, "TIMEOUT", "Timed out");
    when(documentRepository.findByIdForUpdate(pending.id())).thenReturn(pending);
    when(documentRepository.save(any(ElectronicDocument.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    assertThat(service.complete(pending.id(), error).status())
        .isEqualTo(ElectronicDocumentStatus.ERROR);
    verify(draftRepository, never()).save(any());
  }

  private InvoiceDraft approvedDraft() {
    return InvoiceDraft.create(
        "company-1", "customer-1", InvoiceDocumentType.SALES_RECEIPT,
        IdentityDocumentType.DNI, "12345678", "PEN",
        List.of(InvoiceItem.create("Service", BigDecimal.ONE, new BigDecimal("118.00"))),
        Instant.parse("2026-09-01T09:00:00Z"))
        .approve(Instant.parse("2026-09-01T09:30:00Z"));
  }

  private ElectronicDocument document(InvoiceDraft draft) {
    return ElectronicDocument.from(
        draft, new DocumentNumber("B001", 7),
        new IssuerSnapshot("20123456789", "Company SAC", "Company",
            TaxpayerType.LEGAL_ENTITY, "Lima", "150101", "Lima", "Lima", "Lima", "PE"),
        new RecipientSnapshot(IdentityDocumentType.DNI, "12345678", "Customer",
            "Address", "customer@test.pe"));
  }

  private Company company() {
    Company company = new Company();
    company.setId("company-1");
    company.setTaxId("20123456789");
    company.setLegalName("Company SAC");
    company.setTradeName("Company");
    company.setActive(true);
    return company;
  }

  private Customer customer() {
    Customer customer = new Customer();
    customer.setId("customer-1");
    customer.setCompanyId("company-1");
    customer.setFullName("Customer");
    customer.setAddress("Address");
    customer.setEmail("customer@test.pe");
    customer.setActive(true);
    return customer;
  }

  private IssuerTaxProfile profile() {
    Instant now = Instant.parse("2026-09-01T09:00:00Z");
    return new IssuerTaxProfile(
        "company-1", TaxpayerType.LEGAL_ENTITY, "Lima", "150101",
        "Lima", "Lima", "Lima", "PE", now, now);
  }
}
