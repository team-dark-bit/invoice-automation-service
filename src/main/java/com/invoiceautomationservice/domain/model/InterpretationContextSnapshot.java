package com.invoiceautomationservice.domain.model;

import java.util.List;

/**
 * Provider-neutral snapshot of the data already collected during a conversation.
 */
public record InterpretationContextSnapshot(
    InvoiceDocumentType documentType,
    IdentityDocumentType recipientDocumentType,
    String recipientDocumentNumber,
    String currency,
    List<InterpretedInvoiceItem> items
) {
  public InterpretationContextSnapshot {
    recipientDocumentNumber = normalizeOptional(recipientDocumentNumber);
    currency = normalizeCurrency(currency);
    items = items == null ? List.of() : List.copyOf(items);
  }

  public static InterpretationContextSnapshot empty() {
    return new InterpretationContextSnapshot(null, null, null, "PEN", List.of());
  }

  private static String normalizeOptional(String value) {
    return value == null || value.isBlank() ? null : value.strip();
  }

  private static String normalizeCurrency(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    String normalized = value.strip().toUpperCase(java.util.Locale.ROOT);
    if (!normalized.matches("[A-Z]{3}")) {
      throw new IllegalArgumentException("currency must be a three-letter ISO code");
    }
    return normalized;
  }
}
