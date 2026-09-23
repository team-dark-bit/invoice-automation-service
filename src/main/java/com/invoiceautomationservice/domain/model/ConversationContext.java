package com.invoiceautomationservice.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record ConversationContext(
    UUID conversationId, ConversationFlowState state, InvoiceDocumentType documentType,
    IdentityDocumentType recipientDocumentType, String recipientDocumentNumber, String currency,
    List<ConversationDraftItem> items, UUID invoiceDraftId, Instant updatedAt
) {
  public ConversationContext {
    Objects.requireNonNull(conversationId);
    Objects.requireNonNull(state);
    Objects.requireNonNull(currency);
    Objects.requireNonNull(items);
    Objects.requireNonNull(updatedAt);
    items = List.copyOf(items);
  }

  public static ConversationContext empty(UUID conversationId, Instant now) {
    return new ConversationContext(conversationId, ConversationFlowState.EMPTY, null, null, null,
        "PEN", List.of(), null, now);
  }

  public ConversationContext start(InvoiceDocumentType type, IdentityDocumentType identityType,
      String documentNumber, String newCurrency, Instant now) {
    return new ConversationContext(conversationId, ConversationFlowState.COLLECTING_ITEMS, type,
        identityType, documentNumber, newCurrency, List.of(), null, now);
  }

  public ConversationContext addItem(ConversationDraftItem item, Instant now) {
    ensureCollecting();
    var updated = new java.util.ArrayList<>(items);
    updated.add(item);
    return new ConversationContext(conversationId, state, documentType, recipientDocumentType,
        recipientDocumentNumber, currency, updated, null, now);
  }

  public ConversationContext markDraftCreated(UUID draftId, Instant now) {
    ensureCollecting();
    return new ConversationContext(conversationId, ConversationFlowState.DRAFT_CREATED,
        documentType, recipientDocumentType, recipientDocumentNumber, currency, items, draftId, now);
  }

  public ConversationContext reset(Instant now) {
    return empty(conversationId, now);
  }

  public void ensureCollecting() {
    if (state != ConversationFlowState.COLLECTING_ITEMS) {
      throw new IllegalStateException("start a new invoice or receipt first");
    }
  }
}
