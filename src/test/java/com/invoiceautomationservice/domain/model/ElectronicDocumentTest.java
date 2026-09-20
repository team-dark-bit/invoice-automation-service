package com.invoiceautomationservice.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ElectronicDocumentTest {

  @Test
  void snapshotsDraftItemsAndBecomesIssuedWithoutMutatingOriginal() {
    List<InvoiceItem> source = new ArrayList<>(List.of(InvoiceItem.create(
        "Service", UnitCode.NIU, BigDecimal.ONE, new BigDecimal("100"),
        BigDecimal.ZERO, TaxAffectation.TAXED)));
    InvoiceDraft draft = InvoiceDraft.create(
        "company-1", "customer-1", InvoiceDocumentType.SALES_RECEIPT,
        IdentityDocumentType.DNI, "12345678", "PEN", source, Instant.now());
    ElectronicDocument provisional = ElectronicDocument.from(draft, new DocumentNumber("B001", 1));
    source.clear();

    ElectronicDocument issued = provisional.issued(
        new BillingResult("MOCK-B001-00000001", Instant.parse("2026-09-20T10:00:00Z")));

    assertThat(provisional.providerReference()).isNull();
    assertThat(issued.fullNumber()).isEqualTo("B001-00000001");
    assertThat(issued.items()).hasSize(1);
    assertThatThrownBy(() -> issued.items().clear())
        .isInstanceOf(UnsupportedOperationException.class);
  }
}
