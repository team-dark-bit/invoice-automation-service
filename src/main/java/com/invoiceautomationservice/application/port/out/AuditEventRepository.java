package com.invoiceautomationservice.application.port.out;

import com.invoiceautomationservice.application.model.PageQuery;
import com.invoiceautomationservice.application.model.PageResult;
import com.invoiceautomationservice.domain.model.AuditAction;
import com.invoiceautomationservice.domain.model.AuditEvent;
import java.time.Instant;

public interface AuditEventRepository {
  AuditEvent save(AuditEvent event);

  PageResult<AuditEvent> search(
      String companyId, AuditAction action, String resourceType, String resourceId,
      Instant from, Instant to, PageQuery pageQuery);
}
