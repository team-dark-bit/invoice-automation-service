package com.invoiceautomationservice.application.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.invoiceautomationservice.application.dto.request.CreateCompanyMemberRequest;
import com.invoiceautomationservice.application.dto.request.UpdateCompanyMemberRequest;
import com.invoiceautomationservice.application.port.out.CompanyMemberRepository;
import com.invoiceautomationservice.domain.model.CompanyMember;
import com.invoiceautomationservice.domain.model.CompanyPermission;
import com.invoiceautomationservice.domain.model.CompanyRole;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.invoiceautomationservice.infrastructure.config.exception.ApplicationException;

class CompanyMemberServiceTest {
  private CompanyMemberRepository repository;
  private CompanyAccessService accessService;
  private PasswordEncoder passwordEncoder;
  private CompanyMemberService service;

  @BeforeEach
  void setUp() {
    repository = mock(CompanyMemberRepository.class);
    accessService = mock(CompanyAccessService.class);
    passwordEncoder = mock(PasswordEncoder.class);
    service = new CompanyMemberService(repository, accessService, passwordEncoder,
        mock(AuditTrailService.class),
        Clock.fixed(Instant.parse("2026-09-23T15:00:00Z"), ZoneOffset.UTC));
  }

  @Test
  void createsBillingMemberWithHashedPassword() {
    var request = new CreateCompanyMemberRequest(
        "Billing User", "billing", "billing@example.com", "secret123", CompanyRole.BILLING);
    when(passwordEncoder.encode("secret123")).thenReturn("hash");
    when(repository.create(any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(member("user-2", CompanyRole.BILLING, true));

    service.create("company-1", request);

    verify(accessService).requirePermission("company-1", CompanyPermission.MEMBER_MANAGE);
    verify(repository).create("company-1", "Billing User", "billing",
        "billing@example.com", "hash", CompanyRole.BILLING,
        Instant.parse("2026-09-23T15:00:00Z"));
  }

  @Test
  void cannotRemoveLastOwner() {
    when(repository.findByUserId("company-1", "owner-1"))
        .thenReturn(member("owner-1", CompanyRole.OWNER, true));
    when(accessService.currentRole("company-1")).thenReturn(CompanyRole.OWNER);
    when(repository.countOwners("company-1")).thenReturn(1L);

    assertThatThrownBy(() -> service.remove("company-1", "owner-1"))
        .isInstanceOf(ApplicationException.class)
        .hasMessageContaining("at least one active owner");
  }

  @Test
  void adminCannotPromoteMemberToOwner() {
    when(repository.findByUserId("company-1", "user-2"))
        .thenReturn(member("user-2", CompanyRole.BILLING, true));
    when(accessService.currentRole("company-1")).thenReturn(CompanyRole.ADMIN);

    assertThatThrownBy(() -> service.update("company-1", "user-2",
        new UpdateCompanyMemberRequest(CompanyRole.OWNER, null)))
        .isInstanceOf(ApplicationException.class)
        .hasMessageContaining("Only an owner");
  }

  private CompanyMember member(String id, CompanyRole role, boolean active) {
    return new CompanyMember(id, "company-1", "User", "username", "user@example.com",
        active, role, role.permissions(), Instant.parse("2026-09-23T15:00:00Z"));
  }
}
