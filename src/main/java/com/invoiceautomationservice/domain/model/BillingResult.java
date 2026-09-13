package com.invoiceautomationservice.domain.model;

import java.time.Instant;
import java.util.Objects;

public record BillingResult(String reference, Instant issuedAt) {
  public BillingResult {
    Objects.requireNonNull(reference, "reference is required");
    Objects.requireNonNull(issuedAt, "issuedAt is required");
    if (reference.isBlank()) {
      throw new IllegalArgumentException("reference must not be blank");
    }
  }
}
