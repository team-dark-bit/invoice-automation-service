package com.invoiceautomationservice.application.dto.response;

import com.invoiceautomationservice.domain.model.CompanyPermission;
import com.invoiceautomationservice.domain.model.CompanyRole;
import java.time.Instant;
import java.util.Set;

public record CompanyMemberResponse(
    String userId, String companyId, String fullName, String username, String email,
    boolean enabled, CompanyRole role, Set<CompanyPermission> permissions, Instant joinedAt
) {}
