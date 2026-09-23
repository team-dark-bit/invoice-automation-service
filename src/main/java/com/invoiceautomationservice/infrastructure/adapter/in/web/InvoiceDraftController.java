package com.invoiceautomationservice.infrastructure.adapter.in.web;

import com.invoiceautomationservice.application.dto.request.CreateInvoiceDraftRequest;
import com.invoiceautomationservice.application.dto.request.UpdateInvoiceDraftRequest;
import com.invoiceautomationservice.application.dto.response.ElectronicDocumentResponse;
import com.invoiceautomationservice.application.dto.response.InvoiceDraftResponse;
import com.invoiceautomationservice.application.dto.response.PageResponse;
import com.invoiceautomationservice.domain.model.InvoiceDraftStatus;
import com.invoiceautomationservice.application.port.in.InvoiceDraftUseCase;
import com.invoiceautomationservice.commons.response.ApiResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@RestController
@RequestMapping("/api/v1/invoice-drafts")
@RequiredArgsConstructor
@Validated
public class InvoiceDraftController {

  private final InvoiceDraftUseCase invoiceDraftUseCase;

  @PostMapping
  public ResponseEntity<ApiResponse<InvoiceDraftResponse>> create(
      @RequestBody @Valid CreateInvoiceDraftRequest request) {
    InvoiceDraftResponse draft = invoiceDraftUseCase.create(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
        HttpStatus.CREATED.value(), "Invoice draft created successfully", draft));
  }

  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<InvoiceDraftResponse>> findById(@PathVariable UUID id) {
    InvoiceDraftResponse draft = invoiceDraftUseCase.findById(id);
    return ResponseEntity.ok(ApiResponse.success(
        HttpStatus.OK.value(), "Invoice draft found", draft));
  }

  @GetMapping
  public ResponseEntity<ApiResponse<PageResponse<InvoiceDraftResponse>>> search(
      @RequestHeader(value = "X-Company-Id", required = false) String companyId,
      @RequestParam(required = false) InvoiceDraftStatus status,
      @RequestParam(required = false) String recipientDocumentNumber,
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
    var result = invoiceDraftUseCase.search(
        companyId, status, recipientDocumentNumber, page, size);
    return ResponseEntity.ok(ApiResponse.success(
        HttpStatus.OK.value(), "Invoice drafts found", result));
  }

  @PutMapping("/{id}")
  public ResponseEntity<ApiResponse<InvoiceDraftResponse>> update(
      @PathVariable UUID id, @RequestBody @Valid UpdateInvoiceDraftRequest request) {
    InvoiceDraftResponse draft = invoiceDraftUseCase.update(id, request);
    return ResponseEntity.ok(ApiResponse.success(
        HttpStatus.OK.value(), "Invoice draft updated", draft));
  }

  @PostMapping("/{id}/approve")
  public ResponseEntity<ApiResponse<InvoiceDraftResponse>> approve(@PathVariable UUID id) {
    InvoiceDraftResponse draft = invoiceDraftUseCase.approve(id);
    return ResponseEntity.ok(ApiResponse.success(
        HttpStatus.OK.value(), "Invoice draft approved", draft));
  }

  @PostMapping("/{id}/cancel")
  public ResponseEntity<ApiResponse<InvoiceDraftResponse>> cancel(@PathVariable UUID id) {
    InvoiceDraftResponse draft = invoiceDraftUseCase.cancel(id);
    return ResponseEntity.ok(ApiResponse.success(
        HttpStatus.OK.value(), "Invoice draft cancelled", draft));
  }

  @PostMapping("/{id}/issue")
  public ResponseEntity<ApiResponse<ElectronicDocumentResponse>> issue(@PathVariable UUID id) {
    ElectronicDocumentResponse document = invoiceDraftUseCase.issue(id);
    return ResponseEntity.ok(ApiResponse.success(
        HttpStatus.OK.value(), "Electronic document issued", document));
  }
}
