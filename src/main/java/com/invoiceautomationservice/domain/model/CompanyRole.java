package com.invoiceautomationservice.domain.model;

import java.util.EnumSet;
import java.util.Set;

public enum CompanyRole {
  OWNER(EnumSet.allOf(CompanyPermission.class)),
  ADMIN(EnumSet.allOf(CompanyPermission.class)),
  BILLING(EnumSet.of(
      CompanyPermission.COMPANY_READ, CompanyPermission.CUSTOMER_MANAGE,
      CompanyPermission.DRAFT_READ, CompanyPermission.DRAFT_MANAGE,
      CompanyPermission.DOCUMENT_READ, CompanyPermission.DOCUMENT_ISSUE,
      CompanyPermission.DOCUMENT_ADJUST, CompanyPermission.CONVERSATION_READ,
      CompanyPermission.CONVERSATION_MANAGE)),
  VIEWER(EnumSet.of(
      CompanyPermission.COMPANY_READ, CompanyPermission.DRAFT_READ,
      CompanyPermission.DOCUMENT_READ, CompanyPermission.CONVERSATION_READ));

  private final Set<CompanyPermission> permissions;

  CompanyRole(Set<CompanyPermission> permissions) {
    this.permissions = Set.copyOf(permissions);
  }

  public boolean grants(CompanyPermission permission) {
    return permissions.contains(permission);
  }

  public Set<CompanyPermission> permissions() {
    return permissions;
  }
}
