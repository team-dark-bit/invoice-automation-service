package com.invoiceautomationservice.infrastructure.adapter.in.web;

import com.invoiceautomationservice.application.dto.request.CreateInvoiceDraftRequest;
import com.invoiceautomationservice.application.dto.response.InvoiceDraftResponse;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/invoice-drafts")
@RequiredArgsConstructor
public class InvoiceDraftController {

  private final InvoiceDraftUseCase invoiceDraftUseCase;

  @PostMapping
  public ResponseEntity<ApiResponse<InvoiceDraftResponse>> create(
          @RequestBody @Valid CreateInvoiceDraftRequest request
  ) {
    InvoiceDraftResponse draft = invoiceDraftUseCase.create(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(
            ApiResponse.success(HttpStatus.CREATED.value(), "Invoice draft created successfully", draft)
    );
  }

  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<InvoiceDraftResponse>> findById(@PathVariable UUID id) {
    InvoiceDraftResponse draft = invoiceDraftUseCase.findById(id);
    return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Invoice draft found", draft));
  }
}
