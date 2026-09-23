package com.invoiceautomationservice.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class InvoiceDraftTest {

  @Test
  void calculatesTotalsAndProtectsItemsFromExternalMutation() {
    List<InvoiceItem> items = new ArrayList<>(List.of(
            InvoiceItem.create("Consulting", new BigDecimal("2.5000"), new BigDecimal("150.25"))
    ));

    InvoiceDraft draft = InvoiceDraft.create(
            "company-1", "customer-1", InvoiceDocumentType.SALES_RECEIPT,
            IdentityDocumentType.DNI, "12345678", "PEN", items,
            Instant.parse("2026-09-01T10:00:00Z")
    );
    items.clear();

    assertThat(draft.items()).hasSize(1);
    assertThat(draft.subtotal()).isEqualByComparingTo("375.63");
    assertThat(draft.total()).isEqualByComparingTo(draft.subtotal());
    assertThatThrownBy(() -> draft.items().clear()).isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void rejectsDraftWithoutItems() {
    assertThatThrownBy(() -> InvoiceDraft.create(
            "company-1", "customer-1", InvoiceDocumentType.SALES_RECEIPT,
            IdentityDocumentType.DNI, "12345678", "PEN", List.of(), Instant.now()
    )).isInstanceOf(IllegalArgumentException.class)
            .hasMessage("invoice draft must contain at least one item");
  }

  @Test
  void rejectsInvalidCurrency() {
    assertThatThrownBy(() -> InvoiceDraft.create(
            "company-1", "customer-1", InvoiceDocumentType.SALES_RECEIPT,
            IdentityDocumentType.DNI, "12345678", "pen",
            List.of(InvoiceItem.create("Consulting", BigDecimal.ONE, BigDecimal.TEN)), Instant.now()
    )).isInstanceOf(IllegalArgumentException.class)
            .hasMessage("currency must be a three-letter ISO code");
  }

  @Test
  void requiresRucForInvoice() {
    assertThatThrownBy(() -> InvoiceDraft.create(
        "company-1", "customer-1", InvoiceDocumentType.INVOICE,
        IdentityDocumentType.DNI, "12345678", "PEN",
        List.of(InvoiceItem.create("Consulting", BigDecimal.ONE, BigDecimal.TEN)), Instant.now()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("invoice recipient must be identified with RUC");
  }

  @Test
  void rejectsInvalidItemAmounts() {
    assertThatThrownBy(() -> InvoiceItem.create("Consulting", BigDecimal.ZERO, BigDecimal.TEN))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("quantity must be greater than zero");
    assertThatThrownBy(() -> InvoiceItem.create("Consulting", BigDecimal.ONE, BigDecimal.ONE.negate()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("unitPrice must not be negative");
  }

  @Test
  void calculatesDiscountIgvAndTaxTotals() {
    InvoiceItem taxed = InvoiceItem.create(
        "Service", UnitCode.NIU, new BigDecimal("2"), new BigDecimal("100"),
        new BigDecimal("20"), TaxAffectation.TAXED);
    InvoiceDraft draft = InvoiceDraft.create(
        "company-1", "customer-1", InvoiceDocumentType.SALES_RECEIPT,
        IdentityDocumentType.DNI, "12345678", "PEN", List.of(taxed), Instant.now());

    assertThat(taxed.grossAmount()).isEqualByComparingTo("200.00");
    assertThat(taxed.discount()).isEqualByComparingTo("20.00");
    assertThat(taxed.taxableAmount()).isEqualByComparingTo("180.00");
    assertThat(taxed.taxAmount()).isEqualByComparingTo("32.40");
    assertThat(taxed.lineTotal()).isEqualByComparingTo("212.40");
    assertThat(draft.discountTotal()).isEqualByComparingTo("20.00");
    assertThat(draft.taxTotal()).isEqualByComparingTo("32.40");
    assertThat(draft.total()).isEqualByComparingTo("212.40");
  }

  @Test
  void formatsDocumentNumberWithEightDigitCorrelative() {
    assertThat(new DocumentNumber("B001", 25).fullNumber()).isEqualTo("B001-00000025");
  }

  @Test
  void transitionsFromDraftToApprovedAndIssued() {
    Instant createdAt = Instant.parse("2026-09-01T10:00:00Z");
    Instant approvedAt = Instant.parse("2026-09-01T11:00:00Z");
    Instant issuedAt = Instant.parse("2026-09-01T12:00:00Z");
    InvoiceDraft draft = validDraft(createdAt);

    InvoiceDraft approved = draft.approve(approvedAt);
    InvoiceDraft issued = approved.markIssued("MOCK-123", issuedAt);

    assertThat(approved.status()).isEqualTo(InvoiceDraftStatus.APPROVED);
    assertThat(approved.updatedAt()).isEqualTo(approvedAt);
    assertThat(issued.status()).isEqualTo(InvoiceDraftStatus.ISSUED);
    assertThat(issued.providerReference()).isEqualTo("MOCK-123");
    assertThat(issued.issuedAt()).isEqualTo(issuedAt);
    assertThat(issued.updatedAt()).isEqualTo(issuedAt);
  }

  @Test
  void rejectsInvalidStateTransitions() {
    InvoiceDraft draft = validDraft(Instant.parse("2026-09-01T10:00:00Z"));
    InvoiceDraft approved = draft.approve(Instant.parse("2026-09-01T11:00:00Z"));

    assertThatThrownBy(() -> draft.markIssued("MOCK-123", Instant.now()))
            .isInstanceOf(com.invoiceautomationservice.domain.exception.InvalidInvoiceDraftStateException.class);
    assertThatThrownBy(() -> approved.approve(Instant.now()))
            .isInstanceOf(com.invoiceautomationservice.domain.exception.InvalidInvoiceDraftStateException.class);
  }

  @Test
  void editsDraftAndRecalculatesAllTotals() {
    Instant modifiedAt = Instant.parse("2026-09-01T11:00:00Z");
    InvoiceItem updatedItem = InvoiceItem.create(
        "Updated service", UnitCode.NIU, new BigDecimal("2"), new BigDecimal("100"),
        new BigDecimal("20"), TaxAffectation.TAXED);

    InvoiceDraft updated = validDraft(Instant.parse("2026-09-01T10:00:00Z")).update(
        "customer-2", InvoiceDocumentType.INVOICE, IdentityDocumentType.RUC,
        "20123456789", "USD", List.of(updatedItem), modifiedAt);

    assertThat(updated.id()).isNotNull();
    assertThat(updated.customerId()).isEqualTo("customer-2");
    assertThat(updated.documentType()).isEqualTo(InvoiceDocumentType.INVOICE);
    assertThat(updated.currency()).isEqualTo("USD");
    assertThat(updated.subtotal()).isEqualByComparingTo("200.00");
    assertThat(updated.discountTotal()).isEqualByComparingTo("20.00");
    assertThat(updated.taxTotal()).isEqualByComparingTo("32.40");
    assertThat(updated.total()).isEqualByComparingTo("212.40");
    assertThat(updated.updatedAt()).isEqualTo(modifiedAt);
  }

  @Test
  void onlyDraftCanBeEdited() {
    InvoiceDraft approved = validDraft(Instant.parse("2026-09-01T10:00:00Z"))
        .approve(Instant.parse("2026-09-01T11:00:00Z"));

    assertThatThrownBy(() -> approved.update(
        "customer-2", InvoiceDocumentType.SALES_RECEIPT, IdentityDocumentType.DNI,
        "87654321", "PEN", approved.items(), Instant.now()))
        .isInstanceOf(com.invoiceautomationservice.domain.exception
            .InvalidInvoiceDraftStateException.class);
  }

  @Test
  void cancelsDraftOrApprovedDraftButNeverIssuedDraft() {
    Instant now = Instant.parse("2026-09-01T12:00:00Z");
    InvoiceDraft draft = validDraft(Instant.parse("2026-09-01T10:00:00Z"));
    InvoiceDraft approved = draft.approve(Instant.parse("2026-09-01T11:00:00Z"));

    assertThat(draft.cancel(now).status()).isEqualTo(InvoiceDraftStatus.CANCELLED);
    assertThat(approved.cancel(now).status()).isEqualTo(InvoiceDraftStatus.CANCELLED);
    assertThatThrownBy(() -> approved.markIssued("PROVIDER-1", now).cancel(now))
        .isInstanceOf(com.invoiceautomationservice.domain.exception
            .InvalidInvoiceDraftStateException.class);
  }

  private InvoiceDraft validDraft(Instant createdAt) {
    return InvoiceDraft.create(
            "company-1", "customer-1", InvoiceDocumentType.SALES_RECEIPT,
            IdentityDocumentType.DNI, "12345678", "PEN",
            List.of(InvoiceItem.create("Consulting", BigDecimal.ONE, BigDecimal.TEN)), createdAt
    );
  }
}
