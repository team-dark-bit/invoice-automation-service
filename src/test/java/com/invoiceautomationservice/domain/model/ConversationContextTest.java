package com.invoiceautomationservice.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ConversationContextTest {
  @Test
  void collectsItemsAndLinksCreatedDraft() {
    Instant now = Instant.parse("2026-09-23T10:00:00Z");
    ConversationContext context = ConversationContext.empty(UUID.randomUUID(), now)
        .start(InvoiceDocumentType.SALES_RECEIPT, IdentityDocumentType.DNI,
            "12345678", "PEN", now)
        .addItem(ConversationDraftItem.create(
            "Servicio", BigDecimal.ONE, new BigDecimal("100.00")), now);
    UUID draftId = UUID.randomUUID();

    ConversationContext awaiting = context.requestDraftConfirmation(now);
    ConversationContext generated = awaiting.markDraftCreated(draftId, now);

    assertThat(context.state()).isEqualTo(ConversationFlowState.READY_TO_CREATE);
    assertThat(awaiting.state())
        .isEqualTo(ConversationFlowState.AWAITING_DRAFT_CONFIRMATION);
    assertThat(generated.state()).isEqualTo(ConversationFlowState.DRAFT_CREATED);
    assertThat(generated.invoiceDraftId()).isEqualTo(draftId);
    assertThat(generated.items()).hasSize(1);
  }

  @Test
  void cannotAddItemBeforeStartingDocument() {
    ConversationContext context = ConversationContext.empty(
        UUID.randomUUID(), Instant.parse("2026-09-23T10:00:00Z"));
    assertThatThrownBy(() -> context.addItem(ConversationDraftItem.create(
        "Servicio", BigDecimal.ONE, BigDecimal.TEN), context.updatedAt()))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void cannotLinkDraftWithoutPriorConfirmationRequest() {
    Instant now = Instant.parse("2026-09-23T10:00:00Z");
    ConversationContext context = ConversationContext.empty(UUID.randomUUID(), now)
        .start(InvoiceDocumentType.SALES_RECEIPT, IdentityDocumentType.DNI,
            "12345678", "PEN", now)
        .addItem(ConversationDraftItem.create(
            "Servicio", BigDecimal.ONE, BigDecimal.TEN), now);

    assertThatThrownBy(() -> context.markDraftCreated(UUID.randomUUID(), now))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("confirma");
  }

  @Test
  void interpretationCannotChooseTaxAffectation() {
    Instant now = Instant.parse("2026-09-23T10:00:00Z");
    InterpretedInvoiceItem interpreted = new InterpretedInvoiceItem("Servicio", UnitCode.NIU,
        BigDecimal.ONE, BigDecimal.TEN, BigDecimal.ZERO, TaxAffectation.EXEMPT,
        BigDecimal.TEN, BigDecimal.ONE, java.util.List.of());
    DocumentInterpretation interpretation = new DocumentInterpretation(
        InterpretationSource.TEXT, InterpretationIntent.ADD_ITEM, null, null, null, "PEN",
        java.util.List.of(interpreted), BigDecimal.TEN, BigDecimal.ONE,
        java.util.List.of(), java.util.List.of(), java.util.List.of(), java.util.List.of());
    ConversationContext context = ConversationContext.empty(UUID.randomUUID(), now)
        .start(InvoiceDocumentType.SALES_RECEIPT, IdentityDocumentType.DNI,
            "12345678", "PEN", now)
        .applyItems(interpretation, now);

    assertThat(context.items().getFirst().taxAffectation()).isEqualTo(TaxAffectation.TAXED);
  }
}
