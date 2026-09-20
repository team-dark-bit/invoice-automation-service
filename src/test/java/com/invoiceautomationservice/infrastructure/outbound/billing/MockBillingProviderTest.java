package com.invoiceautomationservice.infrastructure.outbound.billing;

import static org.assertj.core.api.Assertions.assertThat;

import com.invoiceautomationservice.domain.model.BillingResult;
import com.invoiceautomationservice.domain.model.InvoiceDraft;
import com.invoiceautomationservice.domain.model.InvoiceItem;
import com.invoiceautomationservice.infrastructure.adapter.out.billing.MockBillingProvider;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

class MockBillingProviderTest {

  @Test
  void returnsDeterministicReferenceAndClockTime() {
    Instant now = Instant.parse("2026-09-01T12:00:00Z");
    MockBillingProvider provider = new MockBillingProvider(Clock.fixed(now, ZoneOffset.UTC));
    InvoiceDraft approved = InvoiceDraft.create(
            "company-1", "customer-1",
            com.invoiceautomationservice.domain.model.InvoiceDocumentType.SALES_RECEIPT,
            com.invoiceautomationservice.domain.model.IdentityDocumentType.DNI,
            "12345678", "PEN",
            List.of(InvoiceItem.create("Consulting", BigDecimal.ONE, BigDecimal.TEN)),
            Instant.parse("2026-09-01T10:00:00Z")
    ).approve(Instant.parse("2026-09-01T11:00:00Z"));

    BillingResult result = provider.issue(approved);

    assertThat(result.reference()).isEqualTo("MOCK-" + approved.id());
    assertThat(result.issuedAt()).isEqualTo(now);
  }
}
