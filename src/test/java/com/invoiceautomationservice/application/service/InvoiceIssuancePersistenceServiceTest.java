package com.invoiceautomationservice.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
import java.time.Clock;
import java.time.ZoneOffset;
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
        customerRepository, profileRepository, accessService,
        Clock.fixed(Instant.parse("2026-09-01T10:00:00Z"), ZoneOffset.UTC));
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
    assertThat(result.document().status()).isEqualTo(ElectronicDocumentStatus.SENDING);
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
    Instant submittedAt = Instant.parse("2026-09-01T10:00:00Z");
    ElectronicDocument pending = document(approved).startSubmission(submittedAt);
    BillingResult providerResult = new BillingResult(
        "provider-1", ElectronicDocumentStatus.ACCEPTED, submittedAt, submittedAt,
        "0", "Accepted");
    when(documentRepository.findByIdForUpdate(pending.id())).thenReturn(pending);
    when(documentRepository.save(any(ElectronicDocument.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(draftRepository.findByIdForUpdate(approved.id())).thenReturn(approved);
    when(draftRepository.save(any(InvoiceDraft.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    ElectronicDocument completed = service.complete(
        pending.id(), pending.submittedAt(), providerResult);

    assertThat(completed.status()).isEqualTo(ElectronicDocumentStatus.ACCEPTED);
    verify(draftRepository).save(org.mockito.ArgumentMatchers.argThat(
        draft -> draft.status() == InvoiceDraftStatus.ISSUED
            && "provider-1".equals(draft.providerReference())));
  }

  @Test
  void errorResultDoesNotMarkDraftIssued() {
    InvoiceDraft approved = approvedDraft();
    Instant failedAt = Instant.parse("2026-09-01T10:00:00Z");
    ElectronicDocument pending = document(approved).startSubmission(failedAt);
    BillingResult error = new BillingResult(
        null, ElectronicDocumentStatus.ERROR, failedAt, failedAt, "TIMEOUT", "Timed out");
    when(documentRepository.findByIdForUpdate(pending.id())).thenReturn(pending);
    when(documentRepository.save(any(ElectronicDocument.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    assertThat(service.complete(pending.id(), pending.submittedAt(), error).status())
        .isEqualTo(ElectronicDocumentStatus.ERROR);
    verify(draftRepository, never()).save(any());
  }

  @Test
  void preparesRecoverableErrorForRetryWithSameDocumentIdentity() {
    InvoiceDraft approved = approvedDraft();
    Instant failedAt = Instant.parse("2026-09-01T09:55:00Z");
    ElectronicDocument failed = document(approved).withBillingResult(new BillingResult(
        null, ElectronicDocumentStatus.ERROR, failedAt, failedAt,
        "PROVIDER_CALL_FAILED", "Timeout"));
    when(documentRepository.findByIdForUpdate(failed.id())).thenReturn(failed);
    when(documentRepository.save(any(ElectronicDocument.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    PreparedEmission retry = service.prepareRetry(failed.id());

    assertThat(retry.document().id()).isEqualTo(failed.id());
    assertThat(retry.document().fullNumber()).isEqualTo(failed.fullNumber());
    assertThat(retry.document().status()).isEqualTo(ElectronicDocumentStatus.SENDING);
    assertThat(retry.document().submittedAt()).isEqualTo(Instant.parse("2026-09-01T10:00:00Z"));
    verifyNoInteractions(seriesRepository);
  }

  @Test
  void rejectsRetryWhileAnotherSubmissionIsStillActive() {
    ElectronicDocument sending = document(approvedDraft())
        .startSubmission(Instant.parse("2026-09-01T09:59:00Z"));
    when(documentRepository.findByIdForUpdate(sending.id())).thenReturn(sending);

    assertThatThrownBy(() -> service.prepareRetry(sending.id()))
        .isInstanceOf(com.invoiceautomationservice.infrastructure.config.exception
            .ApplicationException.class)
        .hasMessageContaining("cannot be retried from status SENDING");
    verify(documentRepository, never()).save(any());
  }

  @Test
  void recoversSubmissionThatHasBeenSendingForFiveMinutes() {
    ElectronicDocument stale = document(approvedDraft())
        .startSubmission(Instant.parse("2026-09-01T09:55:00Z"));
    when(documentRepository.findByIdForUpdate(stale.id())).thenReturn(stale);
    when(documentRepository.save(any(ElectronicDocument.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    PreparedEmission retry = service.prepareRetry(stale.id());

    assertThat(retry.document().status()).isEqualTo(ElectronicDocumentStatus.SENDING);
    assertThat(retry.document().submittedAt()).isEqualTo(Instant.parse("2026-09-01T10:00:00Z"));
  }

  @Test
  void ignoresLateResponseFromAnOlderSubmissionAttempt() {
    ElectronicDocument currentAttempt = document(approvedDraft())
        .startSubmission(Instant.parse("2026-09-01T10:00:00Z"));
    BillingResult lateResult = new BillingResult(
        "provider-old", ElectronicDocumentStatus.ACCEPTED,
        Instant.parse("2026-09-01T09:55:00Z"), Instant.parse("2026-09-01T10:01:00Z"),
        "0", "Accepted");
    when(documentRepository.findByIdForUpdate(currentAttempt.id())).thenReturn(currentAttempt);

    ElectronicDocument result = service.complete(
        currentAttempt.id(), Instant.parse("2026-09-01T09:55:00Z"), lateResult);

    assertThat(result).isSameAs(currentAttempt);
    verify(documentRepository, never()).save(any());
  }

  @Test
  void preparesCreditNoteOnlyOnceAndReservesItsOwnSeries() {
    Instant acceptedAt = Instant.parse("2026-09-01T09:50:00Z");
    ElectronicDocument original = document(approvedDraft()).withBillingResult(new BillingResult(
        "provider-original", ElectronicDocumentStatus.ACCEPTED, acceptedAt, acceptedAt,
        "0", "Accepted"));
    when(documentRepository.findByIdForUpdate(original.id())).thenReturn(original);
    when(documentRepository.findAdjustment(
        original.id(), InvoiceDocumentType.CREDIT_NOTE, "01")).thenReturn(Optional.empty());
    when(seriesRepository.reserveNext("company-1", InvoiceDocumentType.CREDIT_NOTE, "B"))
        .thenReturn(new DocumentNumber("B001", 2));
    when(documentRepository.save(any(ElectronicDocument.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    PreparedEmission prepared = service.prepareAdjustment(
        original.id(), InvoiceDocumentType.CREDIT_NOTE, "01", "Cancellation");

    assertThat(prepared.document().status()).isEqualTo(ElectronicDocumentStatus.SENDING);
    assertThat(prepared.document().relatedDocumentId()).isEqualTo(original.id());
    assertThat(prepared.document().fullNumber()).isEqualTo("B001-00000002");
    assertThat(prepared.document().draftId()).isNull();
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
