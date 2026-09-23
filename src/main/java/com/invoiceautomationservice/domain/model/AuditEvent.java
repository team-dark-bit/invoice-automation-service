package com.invoiceautomationservice.domain.model;

import java.time.Instant;
import java.util.UUID;

public record AuditEvent(
    UUID id,
    String companyId,
    String username,
    AuditAction action,
    String resourceType,
    String resourceId,
    String outcome,
    String detail,
    Instant occurredAt
) {
  public AuditEvent {
    if (id == null || companyId == null || companyId.isBlank()
        || username == null || username.isBlank() || action == null
        || resourceType == null || resourceType.isBlank()
        || resourceId == null || resourceId.isBlank()
        || outcome == null || outcome.isBlank() || occurredAt == null) {
      throw new IllegalArgumentException("audit event required fields must be present");
    }
    detail = detail == null ? null : detail.strip();
  }
}
