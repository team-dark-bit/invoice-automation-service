package com.invoiceautomationservice.infrastructure.adapter.in.web;

import com.invoiceautomationservice.application.dto.response.AuditEventResponse;
import com.invoiceautomationservice.application.dto.response.PageResponse;
import com.invoiceautomationservice.application.service.AuditTrailService;
import com.invoiceautomationservice.commons.response.ApiResponse;
import com.invoiceautomationservice.domain.model.AuditAction;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/audit-events")
@RequiredArgsConstructor
@Validated
public class AuditEventController {
  private final AuditTrailService service;

  @GetMapping
  public ResponseEntity<ApiResponse<PageResponse<AuditEventResponse>>> search(
      @RequestHeader(value = "X-Company-Id", required = false) String companyId,
      @RequestParam(required = false) AuditAction action,
      @RequestParam(required = false) String resourceType,
      @RequestParam(required = false) String resourceId,
      @RequestParam(required = false) Instant from,
      @RequestParam(required = false) Instant to,
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
    return ResponseEntity.ok(ApiResponse.success(200, "Audit events found",
        service.search(companyId, action, resourceType, resourceId, from, to, page, size)));
  }
}
