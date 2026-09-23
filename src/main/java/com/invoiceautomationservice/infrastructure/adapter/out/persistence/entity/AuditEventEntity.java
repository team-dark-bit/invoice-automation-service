package com.invoiceautomationservice.infrastructure.adapter.out.persistence.entity;

import com.invoiceautomationservice.domain.model.AuditAction;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;

@Getter
@Entity
@Table(name = "audit_events")
public class AuditEventEntity {
  @Id private UUID id;
  @Column(name = "company_id", nullable = false) private String companyId;
  @Column(nullable = false) private String username;
  @Enumerated(EnumType.STRING) @Column(nullable = false) private AuditAction action;
  @Column(name = "resource_type", nullable = false) private String resourceType;
  @Column(name = "resource_id", nullable = false) private String resourceId;
  @Column(nullable = false) private String outcome;
  @Column(length = 1000) private String detail;
  @Column(name = "occurred_at", nullable = false) private Instant occurredAt;

  protected AuditEventEntity() {}

  public AuditEventEntity(UUID id, String companyId, String username, AuditAction action,
      String resourceType, String resourceId, String outcome, String detail, Instant occurredAt) {
    this.id = id;
    this.companyId = companyId;
    this.username = username;
    this.action = action;
    this.resourceType = resourceType;
    this.resourceId = resourceId;
    this.outcome = outcome;
    this.detail = detail;
    this.occurredAt = occurredAt;
  }
}
