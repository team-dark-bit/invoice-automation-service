package com.invoiceautomationservice.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
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

  public DocumentInterpretation correctItemQuantity(int index, BigDecimal quantity) {
    InterpretedInvoiceItem current = items.get(index);
    return replaceItem(index, new InterpretedInvoiceItem(current.description(), current.unitCode(),
        quantity, current.unitPrice(), current.discount(), current.taxAffectation(),
        current.reportedTotal(), BigDecimal.ONE, current.warnings()),
        "items[" + index + "].quantity");
  }

  public DocumentInterpretation correctItemUnitPrice(int index, BigDecimal unitPrice) {
    InterpretedInvoiceItem current = items.get(index);
    return replaceItem(index, new InterpretedInvoiceItem(current.description(), current.unitCode(),
        current.quantity(), unitPrice, current.discount(), current.taxAffectation(),
        current.reportedTotal(), BigDecimal.ONE, current.warnings()),
        "items[" + index + "].unitPrice");
  }

  public DocumentInterpretation removeItem(int index) {
    var correctedItems = new ArrayList<>(items);
    correctedItems.remove(index);
    String prefix = "items[" + index + "].";
    List<String> missing = missingFields.stream()
        .filter(field -> !field.startsWith(prefix)).toList();
    List<String> ambiguous = ambiguousFields.stream()
        .filter(field -> !field.startsWith(prefix)).toList();
    return copy(documentType, recipientDocumentType, recipientDocumentNumber, correctedItems,
        missing, ambiguous, List.of(), confidenceAfterCorrection(missing, ambiguous));
  }

  public DocumentInterpretation correctRecipient(
      IdentityDocumentType identityType, String documentNumber) {
    List<String> missing = withoutFields(missingFields,
        "recipientDocumentType", "recipientDocumentNumber");
    List<String> ambiguous = withoutFields(ambiguousFields,
        "recipientDocument", "recipientDocumentType", "recipientDocumentNumber");
    return copy(documentType, identityType, documentNumber, items, missing, ambiguous,
        calculationErrors, confidenceAfterCorrection(missing, ambiguous));
  }

  public DocumentInterpretation correctDocumentType(InvoiceDocumentType type) {
    List<String> missing = withoutFields(missingFields, "documentType");
    List<String> ambiguous = withoutFields(ambiguousFields, "documentType");
    return copy(type, recipientDocumentType, recipientDocumentNumber, items, missing, ambiguous,
        calculationErrors, confidenceAfterCorrection(missing, ambiguous));
  }

  public DocumentInterpretation requireRucRecipient() {
    var missing = new ArrayList<>(missingFields);
    if (!missing.contains("recipientDocumentType")) missing.add("recipientDocumentType");
    if (!missing.contains("recipientDocumentNumber")) missing.add("recipientDocumentNumber");
    return copy(documentType, null, null, items, missing, ambiguousFields,
        calculationErrors, new BigDecimal("0.60"));
  }

  private DocumentInterpretation replaceItem(
      int index, InterpretedInvoiceItem replacement, String correctedField) {
    var correctedItems = new ArrayList<>(items);
    correctedItems.set(index, replacement);
    List<String> missing = withoutFields(missingFields, correctedField,
        correctedField.replace("[" + index + "]", "[0]"));
    List<String> errors = calculationErrors;
    if (reportedTotal != null && replacement.quantity() != null
        && replacement.unitPrice() != null) {
      BigDecimal calculated = replacement.quantity().multiply(replacement.unitPrice())
          .setScale(2, RoundingMode.HALF_UP);
      errors = calculated.compareTo(reportedTotal.setScale(2, RoundingMode.HALF_UP)) == 0
          ? List.of()
          : List.of("El total indicado " + reportedTotal.toPlainString()
              + " no coincide con el total calculado " + calculated.toPlainString());
    }
    return copy(documentType, recipientDocumentType, recipientDocumentNumber, correctedItems,
        missing, ambiguousFields, errors, confidenceAfterCorrection(missing, ambiguousFields));
  }

  private DocumentInterpretation copy(
      InvoiceDocumentType type, IdentityDocumentType identityType, String documentNumber,
      List<InterpretedInvoiceItem> correctedItems, List<String> missing,
      List<String> ambiguous, List<String> errors, BigDecimal correctedConfidence) {
    return new DocumentInterpretation(source, intent, type, identityType, documentNumber,
        currency, correctedItems, reportedTotal, correctedConfidence, missing, ambiguous,
        errors, warnings);
  }

  private static BigDecimal confidenceAfterCorrection(
      List<String> missing, List<String> ambiguous) {
    return missing.isEmpty() && ambiguous.isEmpty()
        ? BigDecimal.ONE : new BigDecimal("0.60");
  }

  private static List<String> withoutFields(List<String> source, String... fields) {
    List<String> removed = List.of(fields);
    return source.stream().filter(value -> !removed.contains(value)).toList();
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
