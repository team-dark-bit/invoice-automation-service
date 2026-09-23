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

    ConversationContext generated = context.markDraftCreated(draftId, now);

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
}
