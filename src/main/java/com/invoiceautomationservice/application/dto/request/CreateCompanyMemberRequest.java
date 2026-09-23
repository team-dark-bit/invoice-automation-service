package com.invoiceautomationservice.application.dto.request;

import com.invoiceautomationservice.domain.model.CompanyRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateCompanyMemberRequest(
    @NotBlank @Size(max = 100) String fullName,
    @NotBlank @Size(min = 3, max = 50) String username,
    @NotBlank @Email @Size(max = 100) String email,
    @NotBlank @Size(min = 8, max = 72) String password,
    @NotNull CompanyRole role
) {}
