package com.invoiceautomationservice.domain.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

public record ConversationContext(
    UUID conversationId, ConversationFlowState state, InvoiceDocumentType documentType,
    IdentityDocumentType recipientDocumentType, String recipientDocumentNumber, String currency,
    List<ConversationDraftItem> items, UUID invoiceDraftId,
    DocumentInterpretation lastInterpretation, boolean reviewRequired, Instant updatedAt
) {
  public ConversationContext {
    Objects.requireNonNull(conversationId, "conversationId is required");
    Objects.requireNonNull(state, "state is required");
    Objects.requireNonNull(currency, "currency is required");
    Objects.requireNonNull(items, "items are required");
    Objects.requireNonNull(updatedAt, "updatedAt is required");
    items = List.copyOf(items);
    recipientDocumentNumber = normalizeOptional(recipientDocumentNumber);
    currency = currency.strip().toUpperCase(Locale.ROOT);
    if (currency.length() != 3) throw new IllegalArgumentException("currency is invalid");
    validateState();
  }

  public static ConversationContext empty(UUID conversationId, Instant now) {
    return new ConversationContext(conversationId, ConversationFlowState.EMPTY, null, null, null,
        "PEN", List.of(), null, null, false, now);
  }

  public ConversationContext start(InvoiceDocumentType type, IdentityDocumentType identityType,
      String documentNumber, String newCurrency, Instant now) {
    Objects.requireNonNull(type, "documentType is required");
    Objects.requireNonNull(identityType, "recipientDocumentType is required");
    return copy(ConversationFlowState.COLLECTING_DATA, type, identityType, documentNumber,
        newCurrency, List.of(), null, null, false, now);
  }

  public ConversationContext applyHeader(DocumentInterpretation interpretation, Instant now) {
    ensureMutable();
    InvoiceDocumentType mergedType = interpretation.documentType() == null
        ? documentType : interpretation.documentType();
    IdentityDocumentType mergedIdentity = interpretation.recipientDocumentType() == null
        ? recipientDocumentType : interpretation.recipientDocumentType();
    String mergedNumber = interpretation.recipientDocumentNumber() == null
        ? recipientDocumentNumber : interpretation.recipientDocumentNumber();
    String mergedCurrency = interpretation.currency() == null ? currency : interpretation.currency();
    ConversationFlowState next = ready(mergedType, mergedIdentity, mergedNumber, items)
        ? ConversationFlowState.READY_TO_CREATE : ConversationFlowState.COLLECTING_DATA;
    return copy(next, mergedType, mergedIdentity, mergedNumber, mergedCurrency, items, null,
        interpretation, false, now);
  }

  public ConversationContext applyItems(DocumentInterpretation interpretation, Instant now) {
    ensureMutable();
    var updated = new ArrayList<>(items);
    for (InterpretedInvoiceItem item : interpretation.items()) {
      updated.add(new ConversationDraftItem(UUID.randomUUID(), item.description(), item.unitCode(),
          item.quantity(), item.unitPrice(), item.discount(), item.taxAffectation()));
    }
    ConversationFlowState next = ready(documentType, recipientDocumentType,
        recipientDocumentNumber, updated)
        ? ConversationFlowState.READY_TO_CREATE : ConversationFlowState.COLLECTING_DATA;
    return copy(next, documentType, recipientDocumentType, recipientDocumentNumber, currency,
        updated, null, interpretation, false, now);
  }

  public ConversationContext addItem(ConversationDraftItem item, Instant now) {
    if (state == ConversationFlowState.EMPTY || state == ConversationFlowState.DRAFT_CREATED
        || state == ConversationFlowState.PROCESSING_MEDIA
        || state == ConversationFlowState.NEEDS_REVIEW) {
      throw new IllegalStateException("Completa o confirma primero los datos del comprobante.");
    }
    var updated = new ArrayList<>(items);
    updated.add(item);
    ConversationFlowState next = ready(documentType, recipientDocumentType,
        recipientDocumentNumber, updated)
        ? ConversationFlowState.READY_TO_CREATE : ConversationFlowState.COLLECTING_DATA;
    return copy(next, documentType, recipientDocumentType, recipientDocumentNumber, currency,
        updated, null, null, false, now);
  }

  public ConversationContext stageInterpretation(
      DocumentInterpretation interpretation, ConversationFlowState nextState, Instant now) {
    ensureMutable();
    Objects.requireNonNull(interpretation, "interpretation is required");
    if (nextState != ConversationFlowState.COLLECTING_DATA
        && nextState != ConversationFlowState.NEEDS_REVIEW) {
      throw new IllegalArgumentException("interpretation can only collect data or need review");
    }
    return copy(nextState, documentType, recipientDocumentType, recipientDocumentNumber, currency,
        items, null, interpretation, nextState == ConversationFlowState.NEEDS_REVIEW, now);
  }

  public ConversationContext markProcessingMedia(Instant now) {
    if (state == ConversationFlowState.DRAFT_CREATED) {
      throw new IllegalStateException("a created draft cannot receive more data");
    }
    return copy(ConversationFlowState.PROCESSING_MEDIA, documentType, recipientDocumentType,
        recipientDocumentNumber, currency, items, null, null, false, now);
  }

  public ConversationContext markMediaFailed(DocumentInterpretation failure, Instant now) {
    return copy(ConversationFlowState.NEEDS_REVIEW, documentType, recipientDocumentType,
        recipientDocumentNumber, currency, items, null, failure, true, now);
  }

  public ConversationContext confirmInterpretation(Instant now) {
    if (state != ConversationFlowState.NEEDS_REVIEW || lastInterpretation == null) {
      throw new IllegalStateException("No hay una interpretación pendiente de confirmación.");
    }
    if (lastInterpretation.hasMissingFields() || !lastInterpretation.ambiguousFields().isEmpty()) {
      throw new IllegalStateException(
          "Los campos faltantes o ambiguos deben corregirse antes de confirmar.");
    }
    return switch (lastInterpretation.intent()) {
      case START_DOCUMENT -> applyHeader(lastInterpretation, now);
      case ADD_ITEM -> applyItems(lastInterpretation, now);
      default -> throw new IllegalStateException(
          "La interpretación pendiente no contiene datos confirmables.");
    };
  }

  public ConversationContext markDraftCreated(UUID draftId, Instant now) {
    ensureReadyToCreate();
    return copy(ConversationFlowState.DRAFT_CREATED, documentType, recipientDocumentType,
        recipientDocumentNumber, currency, items, draftId, lastInterpretation, false, now);
  }

  public ConversationContext reset(Instant now) {
    return empty(conversationId, now);
  }

  public void ensureReadyToCreate() {
    if (state != ConversationFlowState.READY_TO_CREATE) {
      throw new IllegalStateException(
          "La conversación todavía no está lista para crear el borrador.");
    }
  }

  private void ensureMutable() {
    if (state == ConversationFlowState.DRAFT_CREATED) {
      throw new IllegalStateException(
          "Inicia un nuevo comprobante antes de recopilar más datos.");
    }
  }

  private ConversationContext copy(ConversationFlowState newState, InvoiceDocumentType type,
      IdentityDocumentType identityType, String documentNumber, String newCurrency,
      List<ConversationDraftItem> newItems, UUID draftId, DocumentInterpretation interpretation,
      boolean needsReview, Instant now) {
    return new ConversationContext(conversationId, newState, type, identityType, documentNumber,
        newCurrency, newItems, draftId, interpretation, needsReview, now);
  }

  private void validateState() {
    if (state == ConversationFlowState.EMPTY
        && (documentType != null || recipientDocumentType != null
            || recipientDocumentNumber != null || !items.isEmpty() || invoiceDraftId != null)) {
      throw new IllegalArgumentException("empty context cannot contain confirmed data");
    }
    if (state == ConversationFlowState.READY_TO_CREATE
        && !ready(documentType, recipientDocumentType, recipientDocumentNumber, items)) {
      throw new IllegalArgumentException("ready context requires header and items");
    }
    if (state == ConversationFlowState.DRAFT_CREATED && invoiceDraftId == null) {
      throw new IllegalArgumentException("draft-created context requires invoiceDraftId");
    }
    if (state != ConversationFlowState.DRAFT_CREATED && invoiceDraftId != null) {
      throw new IllegalArgumentException("only draft-created context can reference a draft");
    }
    if (reviewRequired != (state == ConversationFlowState.NEEDS_REVIEW)) {
      throw new IllegalArgumentException("reviewRequired must match NEEDS_REVIEW state");
    }
    if (reviewRequired && lastInterpretation == null) {
      throw new IllegalArgumentException("review state requires an interpretation");
    }
  }

  private static boolean ready(InvoiceDocumentType type, IdentityDocumentType identityType,
      String documentNumber, List<ConversationDraftItem> values) {
    return type != null && identityType != null && documentNumber != null && !values.isEmpty();
  }

  private static String normalizeOptional(String value) {
    return value == null || value.isBlank() ? null : value.strip();
  }
}
