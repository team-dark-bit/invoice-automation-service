package com.invoiceautomationservice.domain.model;

import java.math.BigDecimal;
import java.util.List;

/**
 * Candidate item extracted from unstructured input. Nullable business fields represent data that
 * still requires clarification; the backend must validate them before creating an InvoiceItem.
 */
public record InterpretedInvoiceItem(
    String description,
    UnitCode unitCode,
    BigDecimal quantity,
    BigDecimal unitPrice,
    BigDecimal discount,
    TaxAffectation taxAffectation,
    BigDecimal reportedTotal,
    BigDecimal confidence,
    List<String> warnings
) {
  public InterpretedInvoiceItem {
    description = requireText(description, "description");
    requirePositive(quantity, "quantity");
    requireNonNegative(unitPrice, "unitPrice");
    requireNonNegative(discount, "discount");
    requireNonNegative(reportedTotal, "reportedTotal");
    requireConfidence(confidence);
    warnings = warnings == null ? List.of() : List.copyOf(warnings);
  }

  private static String requireText(String value, String field) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(field + " must not be blank");
    }
    return value.strip();
  }

  private static void requirePositive(BigDecimal value, String field) {
    if (value != null && value.signum() <= 0) {
      throw new IllegalArgumentException(field + " must be greater than zero");
    }
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
}
