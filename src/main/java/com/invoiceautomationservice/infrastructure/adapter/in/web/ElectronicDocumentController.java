package com.invoiceautomationservice.infrastructure.adapter.in.web;

import com.invoiceautomationservice.application.dto.response.ElectronicDocumentResponse;
import com.invoiceautomationservice.application.dto.response.PageResponse;
import com.invoiceautomationservice.domain.model.ElectronicDocumentStatus;
import com.invoiceautomationservice.application.service.ElectronicDocumentService;
import com.invoiceautomationservice.commons.response.ApiResponse;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@RestController
@RequestMapping("/api/v1/electronic-documents")
@RequiredArgsConstructor
@Validated
public class ElectronicDocumentController {
  private final ElectronicDocumentService service;

  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<ElectronicDocumentResponse>> findById(@PathVariable UUID id) {
    return ResponseEntity.ok(ApiResponse.success(
        200, "Electronic document found", service.findById(id)));
  }

  @GetMapping
  public ResponseEntity<ApiResponse<PageResponse<ElectronicDocumentResponse>>> search(
      @RequestHeader(value = "X-Company-Id", required = false) String companyId,
      @RequestParam(required = false) ElectronicDocumentStatus status,
      @RequestParam(required = false) String documentNumber,
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
    var result = service.search(companyId, status, documentNumber, page, size);
    return ResponseEntity.ok(ApiResponse.success(
        200, "Electronic documents found", result));
  }

  @PostMapping("/{id}/refresh-status")
  public ResponseEntity<ApiResponse<ElectronicDocumentResponse>> refreshStatus(
      @PathVariable UUID id) {
    ElectronicDocumentResponse document = service.refreshStatus(id);
    return ResponseEntity.ok(ApiResponse.success(
        200, "Electronic document status refreshed", document));
  }

  @PostMapping("/{id}/retry")
  public ResponseEntity<ApiResponse<ElectronicDocumentResponse>> retry(@PathVariable UUID id) {
    ElectronicDocumentResponse document = service.retry(id);
    return ResponseEntity.ok(ApiResponse.success(
        200, "Electronic document submission retried", document));
  }
}
