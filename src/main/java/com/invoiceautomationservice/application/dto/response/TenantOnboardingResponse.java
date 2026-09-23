package com.invoiceautomationservice.application.dto.response;

public record TenantOnboardingResponse(
    CompanyResponse company,
    CompanyMemberResponse owner,
    String nextStep
) {}
