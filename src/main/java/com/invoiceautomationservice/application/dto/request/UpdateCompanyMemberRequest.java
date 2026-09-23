package com.invoiceautomationservice.application.dto.request;

import com.invoiceautomationservice.domain.model.CompanyRole;

public record UpdateCompanyMemberRequest(CompanyRole role, Boolean enabled) {}
