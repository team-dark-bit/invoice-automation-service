package com.invoiceautomationservice.infrastructure.adapter.in.web;

import com.invoiceautomationservice.application.dto.request.CreateCompanyMemberRequest;
import com.invoiceautomationservice.application.dto.request.UpdateCompanyMemberRequest;
import com.invoiceautomationservice.application.dto.request.AddExistingCompanyMemberRequest;
import com.invoiceautomationservice.application.dto.response.CompanyMemberResponse;
import com.invoiceautomationservice.application.service.CompanyMemberService;
import com.invoiceautomationservice.commons.response.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/companies/{companyId}/members")
@RequiredArgsConstructor
public class CompanyMemberController {
  private final CompanyMemberService service;

  @PostMapping
  public ResponseEntity<ApiResponse<CompanyMemberResponse>> create(
      @PathVariable String companyId, @RequestBody @Valid CreateCompanyMemberRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(201,
        "Company member created", service.create(companyId, request)));
  }

  @GetMapping
  public ResponseEntity<ApiResponse<List<CompanyMemberResponse>>> findAll(
      @PathVariable String companyId) {
    return ResponseEntity.ok(ApiResponse.success(200, "Company members found",
        service.findAll(companyId)));
  }

  @PostMapping("/existing")
  public ResponseEntity<ApiResponse<CompanyMemberResponse>> addExisting(
      @PathVariable String companyId,
      @RequestBody @Valid AddExistingCompanyMemberRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(201,
        "Existing user associated", service.addExisting(companyId, request)));
  }

  @PatchMapping("/{userId}")
  public ResponseEntity<ApiResponse<CompanyMemberResponse>> update(
      @PathVariable String companyId, @PathVariable String userId,
      @RequestBody UpdateCompanyMemberRequest request) {
    return ResponseEntity.ok(ApiResponse.success(200, "Company member updated",
        service.update(companyId, userId, request)));
  }

  @DeleteMapping("/{userId}")
  public ResponseEntity<Void> remove(
      @PathVariable String companyId, @PathVariable String userId) {
    service.remove(companyId, userId);
    return ResponseEntity.noContent().build();
  }
}
