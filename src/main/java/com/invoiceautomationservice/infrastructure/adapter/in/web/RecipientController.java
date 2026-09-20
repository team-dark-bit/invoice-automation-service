package com.invoiceautomationservice.infrastructure.adapter.in.web;

import com.invoiceautomationservice.application.dto.request.ResolveRecipientRequest;
import com.invoiceautomationservice.application.dto.response.CustomerResponse;
import com.invoiceautomationservice.application.service.CompanyAccessService;
import com.invoiceautomationservice.application.service.RecipientResolutionService;
import com.invoiceautomationservice.commons.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/recipients")
@RequiredArgsConstructor
public class RecipientController {

  private final CompanyAccessService companyAccessService;
  private final RecipientResolutionService recipientResolutionService;

  @PostMapping("/resolve")
  public ResponseEntity<ApiResponse<CustomerResponse>> resolve(
      @RequestHeader(value = "X-Company-Id", required = false) String requestedCompanyId,
      @RequestBody @Valid ResolveRecipientRequest request) {
    String companyId = companyAccessService.resolveCompanyId(requestedCompanyId);
    CustomerResponse recipient = recipientResolutionService.resolveResponse(
        companyId, request.documentType(), request.documentNumber());
    return ResponseEntity.ok(ApiResponse.success(200, "Recipient resolved", recipient));
  }
}
