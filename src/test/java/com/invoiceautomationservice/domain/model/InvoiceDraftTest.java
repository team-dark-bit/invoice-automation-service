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
            "company-1", "customer-1", "PEN", items, Instant.parse("2026-09-01T10:00:00Z")
    );
    items.clear();

    assertThat(draft.items()).hasSize(1);
    assertThat(draft.subtotal()).isEqualByComparingTo("375.625000");
    assertThat(draft.total()).isEqualByComparingTo(draft.subtotal());
    assertThatThrownBy(() -> draft.items().clear()).isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void rejectsDraftWithoutItems() {
    assertThatThrownBy(() -> InvoiceDraft.create(
            "company-1", "customer-1", "PEN", List.of(), Instant.now()
    )).isInstanceOf(IllegalArgumentException.class)
            .hasMessage("invoice draft must contain at least one item");
  }

  @Test
  void rejectsInvalidCurrency() {
    assertThatThrownBy(() -> InvoiceDraft.create(
            "company-1", "customer-1", "pen",
            List.of(InvoiceItem.create("Consulting", BigDecimal.ONE, BigDecimal.TEN)), Instant.now()
    )).isInstanceOf(IllegalArgumentException.class)
            .hasMessage("currency must be a three-letter ISO code");
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

  private InvoiceDraft validDraft(Instant createdAt) {
    return InvoiceDraft.create(
            "company-1", "customer-1", "PEN",
            List.of(InvoiceItem.create("Consulting", BigDecimal.ONE, BigDecimal.TEN)), createdAt
    );
  }
}
