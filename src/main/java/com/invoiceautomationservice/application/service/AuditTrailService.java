package com.invoiceautomationservice.application.service;

import com.invoiceautomationservice.application.dto.response.AuditEventResponse;
import com.invoiceautomationservice.application.dto.response.PageResponse;
import com.invoiceautomationservice.application.model.PageQuery;
import com.invoiceautomationservice.application.port.out.AuditEventRepository;
import com.invoiceautomationservice.application.port.out.CurrentUserProvider;
import com.invoiceautomationservice.domain.model.AuditAction;
import com.invoiceautomationservice.domain.model.AuditEvent;
import com.invoiceautomationservice.domain.model.CompanyPermission;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuditTrailService {
  private final AuditEventRepository repository;
  private final CurrentUserProvider currentUserProvider;
  private final CompanyAccessService companyAccessService;
  private final Clock clock;

  @Transactional
  public void record(
      String companyId, AuditAction action, String resourceType, Object resourceId,
      String outcome, String detail) {
    repository.save(new AuditEvent(
        UUID.randomUUID(), companyId, currentUserProvider.username(), action, resourceType,
        String.valueOf(resourceId), outcome, detail, Instant.now(clock)));
  }

  @Transactional
  public void recordAs(String username, String companyId, AuditAction action,
      String resourceType, Object resourceId, String outcome, String detail) {
    repository.save(new AuditEvent(
        UUID.randomUUID(), companyId, username, action, resourceType, String.valueOf(resourceId),
        outcome, detail, Instant.now(clock)));
  }

  @Transactional(readOnly = true)
  public PageResponse<AuditEventResponse> search(
      String requestedCompanyId, AuditAction action, String resourceType, String resourceId,
      Instant from, Instant to, int page, int size) {
    String companyId = companyAccessService.resolveCompanyId(requestedCompanyId);
    companyAccessService.requirePermission(companyId, CompanyPermission.AUDIT_READ);
    if (from != null && to != null && from.isAfter(to)) {
      throw new IllegalArgumentException("from must be before or equal to to");
    }
    return repository.search(companyId, action, resourceType, resourceId, from, to,
        new PageQuery(page, size)).map(this::toResponse);
  }

  private AuditEventResponse toResponse(AuditEvent event) {
    return new AuditEventResponse(event.id(), event.companyId(), event.username(), event.action(),
        event.resourceType(), event.resourceId(), event.outcome(), event.detail(), event.occurredAt());
  }
}
