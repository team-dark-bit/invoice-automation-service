package com.invoiceautomationservice.domain.model;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

public record InvoiceItem(
        UUID id,
        String description,
        BigDecimal quantity,
        BigDecimal unitPrice,
        BigDecimal lineTotal
) {

  public InvoiceItem {
    Objects.requireNonNull(id, "id is required");
    Objects.requireNonNull(description, "description is required");
    Objects.requireNonNull(quantity, "quantity is required");
    Objects.requireNonNull(unitPrice, "unitPrice is required");
    Objects.requireNonNull(lineTotal, "lineTotal is required");
    if (description.isBlank()) {
      throw new IllegalArgumentException("description must not be blank");
    }
    if (quantity.signum() <= 0) {
      throw new IllegalArgumentException("quantity must be greater than zero");
    }
    if (unitPrice.signum() < 0) {
      throw new IllegalArgumentException("unitPrice must not be negative");
    }
  }

  public static InvoiceItem create(String description, BigDecimal quantity, BigDecimal unitPrice) {
    return new InvoiceItem(
            UUID.randomUUID(), description.strip(), quantity, unitPrice, quantity.multiply(unitPrice)
    );
  }
}
