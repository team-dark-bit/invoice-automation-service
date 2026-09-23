package com.invoiceautomationservice.domain.model;

import java.time.Instant;
import java.util.Set;

public record CompanyMember(
    String userId, String companyId, String fullName, String username, String email,
    boolean enabled, CompanyRole role, Set<CompanyPermission> permissions, Instant joinedAt
) {
  public CompanyMember {
    permissions = Set.copyOf(permissions);
  }
}
