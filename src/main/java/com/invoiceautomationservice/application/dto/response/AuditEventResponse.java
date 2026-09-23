package com.invoiceautomationservice.application.dto.response;

import com.invoiceautomationservice.domain.model.AuditAction;
import java.time.Instant;
import java.util.UUID;

public record AuditEventResponse(
    UUID id,
    String companyId,
    String username,
    AuditAction action,
    String resourceType,
    String resourceId,
    String outcome,
    String detail,
    Instant occurredAt
) {}
