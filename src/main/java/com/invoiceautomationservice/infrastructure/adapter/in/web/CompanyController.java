package com.invoiceautomationservice.infrastructure.adapter.in.web;

import com.invoiceautomationservice.application.dto.request.CreateCompanyRequest;
import com.invoiceautomationservice.application.dto.response.CompanyResponse;
import com.invoiceautomationservice.application.port.in.CompanyUseCase;
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
}
