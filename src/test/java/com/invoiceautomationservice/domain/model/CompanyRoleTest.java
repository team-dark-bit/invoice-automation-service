package com.invoiceautomationservice.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CompanyRoleTest {
  @Test
  void ownerHasEveryPermission() {
    assertThat(CompanyRole.OWNER.permissions()).containsExactlyInAnyOrder(
        CompanyPermission.values());
  }

  @Test
  void billingCanIssueButCannotManageMembersOrOnboarding() {
    assertThat(CompanyRole.BILLING.grants(CompanyPermission.DOCUMENT_ISSUE)).isTrue();
    assertThat(CompanyRole.BILLING.grants(CompanyPermission.MEMBER_MANAGE)).isFalse();
    assertThat(CompanyRole.BILLING.grants(CompanyPermission.ONBOARDING_MANAGE)).isFalse();
  }

  @Test
  void viewerHasReadOnlyAccess() {
    assertThat(CompanyRole.VIEWER.grants(CompanyPermission.DOCUMENT_READ)).isTrue();
    assertThat(CompanyRole.VIEWER.grants(CompanyPermission.DRAFT_MANAGE)).isFalse();
  }
}
