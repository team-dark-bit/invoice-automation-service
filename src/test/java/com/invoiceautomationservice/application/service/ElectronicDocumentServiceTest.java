package com.invoiceautomationservice.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.invoiceautomationservice.application.port.out.BillingProvider;
import com.invoiceautomationservice.application.port.out.ElectronicDocumentRepository;
import com.invoiceautomationservice.domain.model.BillingResult;
import com.invoiceautomationservice.domain.model.DocumentNumber;
import com.invoiceautomationservice.domain.model.ElectronicDocument;
import com.invoiceautomationservice.domain.model.ElectronicDocumentStatus;
import com.invoiceautomationservice.domain.model.IdentityDocumentType;
import com.invoiceautomationservice.domain.model.InvoiceDocumentType;
import com.invoiceautomationservice.domain.model.InvoiceDraft;
import com.invoiceautomationservice.domain.model.InvoiceItem;
import com.invoiceautomationservice.domain.model.IssuerSnapshot;
import com.invoiceautomationservice.domain.model.RecipientSnapshot;
import com.invoiceautomationservice.domain.model.TaxpayerType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class ElectronicDocumentServiceTest {

  @Test
  void refreshesSentDocumentUntilProviderAcceptsIt() {
    ElectronicDocumentRepository repository = mock(ElectronicDocumentRepository.class);
    CompanyAccessService accessService = mock(CompanyAccessService.class);
    BillingProvider provider = mock(BillingProvider.class);
    ElectronicDocumentService service = new ElectronicDocumentService(
        repository, accessService, provider,
        mock(InvoiceIssuancePersistenceService.class), mock(BillingSubmissionService.class),
        mock(AuditTrailService.class));
    Instant submittedAt = Instant.parse("2026-09-20T10:00:00Z");
    ElectronicDocument sent = provisional().withBillingResult(new BillingResult(
        "PROVIDER-123", ElectronicDocumentStatus.SENT, submittedAt,
        null, "98", "Processing"));
    BillingResult accepted = new BillingResult(
        "PROVIDER-123", ElectronicDocumentStatus.ACCEPTED, submittedAt,
        Instant.parse("2026-09-20T10:01:00Z"), "0", "Accepted");
    when(repository.findById(sent.id())).thenReturn(sent);
    when(provider.checkStatus("PROVIDER-123")).thenReturn(accepted);
    when(repository.save(org.mockito.ArgumentMatchers.any(ElectronicDocument.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    var response = service.refreshStatus(sent.id());

    assertThat(response.status()).isEqualTo(ElectronicDocumentStatus.ACCEPTED);
    assertThat(response.submittedAt()).isEqualTo(submittedAt);
    assertThat(response.providerResponseCode()).isEqualTo("0");
    verify(accessService).requireAccess("company-1");
    verify(provider).checkStatus("PROVIDER-123");
  }

  @Test
  void retriesDocumentThroughSeparatedPersistenceAndProviderPhases() {
    ElectronicDocumentRepository repository = mock(ElectronicDocumentRepository.class);
    CompanyAccessService accessService = mock(CompanyAccessService.class);
    BillingProvider provider = mock(BillingProvider.class);
    InvoiceIssuancePersistenceService persistence =
        mock(InvoiceIssuancePersistenceService.class);
    BillingSubmissionService submission = mock(BillingSubmissionService.class);
    ElectronicDocumentService service = new ElectronicDocumentService(
        repository, accessService, provider, persistence, submission,
        mock(AuditTrailService.class));
    ElectronicDocument sending = provisional()
        .startSubmission(Instant.parse("2026-09-20T10:00:00Z"));
    ElectronicDocument accepted = sending.withBillingResult(new BillingResult(
        "PROVIDER-123", ElectronicDocumentStatus.ACCEPTED,
        Instant.parse("2026-09-20T10:00:00Z"),
        Instant.parse("2026-09-20T10:00:01Z"), "0", "Accepted"));
    when(persistence.prepareRetry(sending.id()))
        .thenReturn(new PreparedEmission(sending, true));
    when(submission.submit(sending)).thenReturn(accepted);

    var response = service.retry(sending.id());

    assertThat(response.status()).isEqualTo(ElectronicDocumentStatus.ACCEPTED);
    verify(persistence).prepareRetry(sending.id());
    verify(submission).submit(sending);
  }

  private ElectronicDocument provisional() {
    InvoiceDraft draft = InvoiceDraft.create(
        "company-1", "customer-1", InvoiceDocumentType.SALES_RECEIPT,
        IdentityDocumentType.DNI, "12345678", "PEN",
        List.of(InvoiceItem.create("Service", BigDecimal.ONE, BigDecimal.TEN)),
        Instant.parse("2026-09-20T09:00:00Z"));
    IssuerSnapshot issuer = new IssuerSnapshot(
        "20123456789", "Company SAC", "Company", TaxpayerType.LEGAL_ENTITY,
        "Lima", "150101", "Lima", "Lima", "Lima", "PE");
    RecipientSnapshot recipient = new RecipientSnapshot(
        IdentityDocumentType.DNI, "12345678", "Customer", null, null);
    return ElectronicDocument.from(draft, new DocumentNumber("B001", 1), issuer, recipient);
  }
}
