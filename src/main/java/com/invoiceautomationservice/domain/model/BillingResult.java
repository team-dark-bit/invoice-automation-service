package com.invoiceautomationservice.domain.model;

import java.time.Instant;

public record BillingResult(
    String reference,
    ElectronicDocumentStatus status,
    Instant submittedAt,
    Instant respondedAt,
    String responseCode,
    String responseMessage
) {
  public BillingResult {
    if (status == null || submittedAt == null) {
      throw new IllegalArgumentException("status and submittedAt are required");
    }
    if (status != ElectronicDocumentStatus.ERROR
        && (reference == null || reference.isBlank())) {
      throw new IllegalArgumentException("provider reference is required");
    }
    if ((status == ElectronicDocumentStatus.ACCEPTED
        || status == ElectronicDocumentStatus.REJECTED) && respondedAt == null) {
      throw new IllegalArgumentException("respondedAt is required for a final status");
    }
  }
}
