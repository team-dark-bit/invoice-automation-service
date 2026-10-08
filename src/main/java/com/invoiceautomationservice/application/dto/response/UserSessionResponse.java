package com.invoiceautomationservice.application.dto.response;

import com.invoiceautomationservice.domain.model.CompanyPermission;
import com.invoiceautomationservice.domain.model.CompanyRole;
import java.util.List;
import java.util.Set;

public record UserSessionResponse(
    String userId,
    String fullName,
    String username,
    String email,
    String defaultCompanyId,
    List<CompanyAccess> companies
) {
  public record CompanyAccess(
      String id,
      String legalName,
      String tradeName,
      String taxId,
      boolean active,
      CompanyRole role,
      Set<CompanyPermission> permissions
  ) {}
}
