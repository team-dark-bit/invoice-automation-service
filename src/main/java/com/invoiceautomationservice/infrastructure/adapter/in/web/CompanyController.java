package com.invoiceautomationservice.infrastructure.adapter.in.web;

import com.invoiceautomationservice.application.dto.request.CreateCompanyRequest;
import com.invoiceautomationservice.application.dto.response.CompanyResponse;
import com.invoiceautomationservice.application.dto.response.PageResponse;
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
import org.springframework.web.bind.annotation.RequestParam;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import com.invoiceautomationservice.application.dto.request.UpdateCompanyRequest;
import com.invoiceautomationservice.application.dto.request.UpdateDocumentSeriesRequest;
import com.invoiceautomationservice.application.dto.response.OnboardingStatusResponse;
import com.invoiceautomationservice.application.service.OnboardingStatusService;

@RestController
@RequestMapping("/api/v1/companies")
@Validated
@RequiredArgsConstructor
public class CompanyController {

  private final CompanyUseCase companyUseCase;
  private final IssuerOnboardingUseCase issuerOnboardingUseCase;
  private final DocumentSeriesService documentSeriesService;
  private final OnboardingStatusService onboardingStatusService;

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

  @GetMapping("/search")
  public ResponseEntity<ApiResponse<PageResponse<CompanyResponse>>> search(
      @RequestParam(required = false) String query,
      @RequestParam(required = false) Boolean active,
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
    return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Companies found",
        companyUseCase.search(query, active, page, size)));
  }

  @PatchMapping("/{id}")
  public ResponseEntity<ApiResponse<CompanyResponse>> update(
      @PathVariable String id, @RequestBody @Valid UpdateCompanyRequest request) {
    return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Company updated",
        companyUseCase.update(id, request)));
  }

  @PostMapping("/{id}/tax-profile")
  public ResponseEntity<ApiResponse<IssuerTaxProfileResponse>> configureTaxProfile(
      @PathVariable String id,
      @RequestBody @Valid ConfigureIssuerTaxProfileRequest request) {
    IssuerTaxProfileResponse profile = issuerOnboardingUseCase.configure(id, request);
    return ResponseEntity.ok(ApiResponse.success(
        HttpStatus.OK.value(), "Issuer tax profile configured", profile));
  }

  @PutMapping("/{id}/tax-profile")
  public ResponseEntity<ApiResponse<IssuerTaxProfileResponse>> updateTaxProfile(
      @PathVariable String id,
      @RequestBody @Valid ConfigureIssuerTaxProfileRequest request) {
    return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(),
        "Issuer tax profile updated", issuerOnboardingUseCase.configure(id, request)));
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

  @PatchMapping("/{id}/document-series/{seriesId}")
  public ResponseEntity<ApiResponse<DocumentSeriesResponse>> updateDocumentSeries(
      @PathVariable String id, @PathVariable String seriesId,
      @RequestBody @Valid UpdateDocumentSeriesRequest request) {
    return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Document series updated",
        documentSeriesService.update(id, seriesId, request)));
  }

  @DeleteMapping("/{id}/document-series/{seriesId}")
  public ResponseEntity<ApiResponse<Void>> deleteDocumentSeries(
      @PathVariable String id, @PathVariable String seriesId) {
    documentSeriesService.delete(id, seriesId);
    return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(),
        "Document series deleted", null));
  }

  @GetMapping("/{id}/onboarding-status")
  public ResponseEntity<ApiResponse<OnboardingStatusResponse>> onboardingStatus(
      @PathVariable String id) {
    return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Onboarding status found",
        onboardingStatusService.get(id)));
  }
}
