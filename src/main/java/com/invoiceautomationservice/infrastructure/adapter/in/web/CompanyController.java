package com.invoiceautomationservice.infrastructure.adapter.in.web;

import com.invoiceautomationservice.application.dto.request.CreateCompanyRequest;
import com.invoiceautomationservice.application.dto.response.CompanyResponse;
import com.invoiceautomationservice.application.dto.request.ConfigureIssuerTaxProfileRequest;
import com.invoiceautomationservice.application.dto.response.IssuerTaxProfileResponse;
import com.invoiceautomationservice.application.port.in.CompanyUseCase;
import com.invoiceautomationservice.application.port.in.IssuerOnboardingUseCase;
import com.invoiceautomationservice.application.dto.request.ConfigureDocumentSeriesRequest;
import com.invoiceautomationservice.application.dto.response.DocumentSeriesResponse;
import com.invoiceautomationservice.application.service.DocumentSeriesService;
import com.invoiceautomationservice.commons.response.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/companies")
@Validated
@RequiredArgsConstructor
public class CompanyController {

  private final CompanyUseCase companyUseCase;
  private final IssuerOnboardingUseCase issuerOnboardingUseCase;
  private final DocumentSeriesService documentSeriesService;

  @PostMapping
  public ResponseEntity<ApiResponse<CompanyResponse>> create(
          @RequestBody @Valid CreateCompanyRequest request
  ) {
    CompanyResponse company = companyUseCase.create(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(
            ApiResponse.success(HttpStatus.CREATED.value(), "Company created successfully", company)
    );
  }

  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<CompanyResponse>> findById(@PathVariable String id) {
    CompanyResponse company = companyUseCase.findById(id);
    return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Company found", company));
  }

  @GetMapping
  public ResponseEntity<ApiResponse<List<CompanyResponse>>> findAll() {
    List<CompanyResponse> companies = companyUseCase.findAll();
    return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Companies found", companies));
  }

  @PostMapping("/{id}/tax-profile")
  public ResponseEntity<ApiResponse<IssuerTaxProfileResponse>> configureTaxProfile(
      @PathVariable String id,
      @RequestBody @Valid ConfigureIssuerTaxProfileRequest request) {
    IssuerTaxProfileResponse profile = issuerOnboardingUseCase.configure(id, request);
    return ResponseEntity.ok(ApiResponse.success(
        HttpStatus.OK.value(), "Issuer tax profile configured", profile));
  }

  @GetMapping("/{id}/tax-profile")
  public ResponseEntity<ApiResponse<IssuerTaxProfileResponse>> findTaxProfile(
      @PathVariable String id) {
    IssuerTaxProfileResponse profile = issuerOnboardingUseCase.findByCompanyId(id);
    return ResponseEntity.ok(ApiResponse.success(
        HttpStatus.OK.value(), "Issuer tax profile found", profile));
  }

  @PostMapping("/{id}/document-series")
  public ResponseEntity<ApiResponse<DocumentSeriesResponse>> configureDocumentSeries(
      @PathVariable String id,
      @RequestBody @Valid ConfigureDocumentSeriesRequest request) {
    DocumentSeriesResponse series = documentSeriesService.configure(id, request);
    return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
        HttpStatus.CREATED.value(), "Document series configured", series));
  }

  @GetMapping("/{id}/document-series")
  public ResponseEntity<ApiResponse<List<DocumentSeriesResponse>>> findDocumentSeries(
      @PathVariable String id) {
    List<DocumentSeriesResponse> series = documentSeriesService.findAll(id);
    return ResponseEntity.ok(ApiResponse.success(
        HttpStatus.OK.value(), "Document series found", series));
  }
}
