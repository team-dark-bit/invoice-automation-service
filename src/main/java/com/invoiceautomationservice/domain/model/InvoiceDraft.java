package com.invoiceautomationservice.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record InvoiceDraft(
        UUID id,
        String companyId,
        String customerId,
        String currency,
        InvoiceDraftStatus status,
        List<InvoiceItem> items,
        BigDecimal subtotal,
        BigDecimal total,
        Instant createdAt,
        Instant updatedAt
) {

  public InvoiceDraft {
    Objects.requireNonNull(id, "id is required");
    Objects.requireNonNull(companyId, "companyId is required");
    Objects.requireNonNull(customerId, "customerId is required");
    Objects.requireNonNull(currency, "currency is required");
    Objects.requireNonNull(status, "status is required");
    Objects.requireNonNull(items, "items are required");
    Objects.requireNonNull(subtotal, "subtotal is required");
    Objects.requireNonNull(total, "total is required");
    Objects.requireNonNull(createdAt, "createdAt is required");
    Objects.requireNonNull(updatedAt, "updatedAt is required");
    items = List.copyOf(items);
    if (!currency.matches("[A-Z]{3}")) {
      throw new IllegalArgumentException("currency must be a three-letter ISO code");
    }
    if (items.isEmpty()) {
      throw new IllegalArgumentException("invoice draft must contain at least one item");
    }
  }

  public static InvoiceDraft create(
          String companyId,
          String customerId,
          String currency,
          List<InvoiceItem> items,
          Instant createdAt
  ) {
    BigDecimal subtotal = items.stream()
            .map(InvoiceItem::lineTotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    return new InvoiceDraft(
            UUID.randomUUID(), companyId, customerId, currency, InvoiceDraftStatus.DRAFT,
            items, subtotal, subtotal, createdAt, createdAt
    );
  }
}
