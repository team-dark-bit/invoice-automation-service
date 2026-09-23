package com.invoiceautomationservice.infrastructure.outbound.billing;

import static org.assertj.core.api.Assertions.assertThat;

import com.invoiceautomationservice.domain.model.BillingResult;
import com.invoiceautomationservice.domain.model.BillingSubmission;
import com.invoiceautomationservice.domain.model.ElectronicDocumentStatus;
import com.invoiceautomationservice.domain.model.IdentityDocumentType;
import com.invoiceautomationservice.domain.model.InvoiceDocumentType;
import com.invoiceautomationservice.domain.model.TaxpayerType;
import com.invoiceautomationservice.infrastructure.adapter.out.billing.MockBillingProvider;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MockBillingProviderTest {

  @Test
  void returnsDeterministicReferenceAndClockTime() {
    Instant now = Instant.parse("2026-09-01T12:00:00Z");
    MockBillingProvider provider = new MockBillingProvider(Clock.fixed(now, ZoneOffset.UTC));
    BillingSubmission submission = new BillingSubmission(
        UUID.randomUUID(), "idempotency-key", "B001-00000001", "B001", 1,
        InvoiceDocumentType.SALES_RECEIPT, "generar_comprobante", "1", now, "PEN", "1",
        new BillingSubmission.Issuer("20123456789", "Issuer SAC", "Issuer",
            TaxpayerType.LEGAL_ENTITY, "Lima",
            "150101", "Lima", "Lima", "Lima", "PE"),
        new BillingSubmission.Recipient(
            IdentityDocumentType.DNI, "12345678", "Customer", "Lima", "customer@test.pe"),
        List.of(), BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.TEN,
        BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.TEN,
        new BigDecimal("18.00"), true, false);
    BillingResult result = provider.submit(submission);

    assertThat(result.reference()).isEqualTo("MOCK-B001-00000001");
    assertThat(result.status()).isEqualTo(ElectronicDocumentStatus.ACCEPTED);
    assertThat(result.submittedAt()).isEqualTo(now);
  }
}
