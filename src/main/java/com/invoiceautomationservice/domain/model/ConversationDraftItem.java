package com.invoiceautomationservice.domain.model;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

public record ConversationDraftItem(
    UUID id, String description, UnitCode unitCode, BigDecimal quantity,
    BigDecimal unitPrice, BigDecimal discount, TaxAffectation taxAffectation
) {
  public ConversationDraftItem {
    Objects.requireNonNull(id);
    Objects.requireNonNull(description);
    Objects.requireNonNull(unitCode);
    Objects.requireNonNull(quantity);
    Objects.requireNonNull(unitPrice);
    Objects.requireNonNull(discount);
    Objects.requireNonNull(taxAffectation);
    description = description.strip();
    if (description.isBlank() || quantity.signum() <= 0 || unitPrice.signum() < 0
        || discount.signum() < 0) {
      throw new IllegalArgumentException("invalid conversational item");
    }
  }

  public static ConversationDraftItem create(
      String description, BigDecimal quantity, BigDecimal unitPrice) {
    return new ConversationDraftItem(UUID.randomUUID(), description, UnitCode.NIU, quantity,
        unitPrice, BigDecimal.ZERO, TaxAffectation.TAXED);
  }
}
