package com.invoiceautomationservice.domain.model;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Structured candidate extracted from text or an image. It is never an authorization to create,
 * approve or issue a document; application services must validate and confirm it first.
 */
public record DocumentInterpretation(
    InterpretationSource source,
    InterpretationIntent intent,
    InvoiceDocumentType documentType,
    IdentityDocumentType recipientDocumentType,
    String recipientDocumentNumber,
    String currency,
    List<InterpretedInvoiceItem> items,
    BigDecimal reportedTotal,
    BigDecimal confidence,
    List<String> missingFields,
    List<String> ambiguousFields,
    List<String> calculationErrors,
    List<String> warnings
) {
  public DocumentInterpretation {
    Objects.requireNonNull(source, "source must not be null");
    Objects.requireNonNull(intent, "intent must not be null");
    recipientDocumentNumber = normalizeOptional(recipientDocumentNumber);
    currency = normalizeCurrency(currency);
    items = items == null ? List.of() : List.copyOf(items);
    requireNonNegative(reportedTotal, "reportedTotal");
    requireConfidence(confidence);
    missingFields = copyNonBlank(missingFields, "missingFields");
    ambiguousFields = copyNonBlank(ambiguousFields, "ambiguousFields");
    calculationErrors = copyNonBlank(calculationErrors, "calculationErrors");
    warnings = copyNonBlank(warnings, "warnings");
  }

  public static DocumentInterpretation unsupported(InterpretationSource source, String warning) {
    return new DocumentInterpretation(source, InterpretationIntent.UNKNOWN, null, null, null,
        null, List.of(), null, BigDecimal.ZERO, List.of(), List.of(), List.of(), List.of(warning));
  }

  public boolean requiresReview(BigDecimal minimumConfidence) {
    Objects.requireNonNull(minimumConfidence, "minimumConfidence must not be null");
    return confidence.compareTo(minimumConfidence) < 0
        || !ambiguousFields.isEmpty() || !calculationErrors.isEmpty();
  }

  public boolean hasMissingFields() {
    return !missingFields.isEmpty();
  }

  private static String normalizeOptional(String value) {
    return value == null || value.isBlank() ? null : value.strip();
  }

  private static String normalizeCurrency(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    String normalized = value.strip().toUpperCase(Locale.ROOT);
    if (!normalized.matches("[A-Z]{3}")) {
      throw new IllegalArgumentException("currency must be a three-letter ISO code");
    }
    return normalized;
  }

  private static void requireNonNegative(BigDecimal value, String field) {
    if (value != null && value.signum() < 0) {
      throw new IllegalArgumentException(field + " must not be negative");
    }
  }

  private static void requireConfidence(BigDecimal value) {
    if (value == null || value.compareTo(BigDecimal.ZERO) < 0
        || value.compareTo(BigDecimal.ONE) > 0) {
      throw new IllegalArgumentException("confidence must be between zero and one");
    }
  }

  private static List<String> copyNonBlank(List<String> values, String field) {
    if (values == null) {
      return List.of();
    }
    return values.stream().map(value -> {
      if (value == null || value.isBlank()) {
        throw new IllegalArgumentException(field + " must not contain blank values");
      }
      return value.strip();
    }).toList();
  }
}
