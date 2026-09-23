package com.invoiceautomationservice.application.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TenantOnboardingRequest(
    @NotBlank @Size(max = 100) String fullName,
    @NotBlank @Size(min = 3, max = 50) String username,
    @NotBlank @Email @Size(max = 100) String email,
    @NotBlank @Size(min = 8, max = 72) String password,
    @Valid Company company
) {
  public record Company(
      @NotBlank @Size(max = 150) String legalName,
      @Size(max = 150) String tradeName,
      @NotBlank @jakarta.validation.constraints.Pattern(regexp = "\\d{11}") String taxId,
      @Size(max = 250) String address
  ) {}
}
