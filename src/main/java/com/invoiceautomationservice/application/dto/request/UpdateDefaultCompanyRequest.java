package com.invoiceautomationservice.application.dto.request;

import jakarta.validation.constraints.NotBlank;

public record UpdateDefaultCompanyRequest(@NotBlank String companyId) {}
