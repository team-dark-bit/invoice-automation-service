package com.invoiceautomationservice.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.invoiceautomationservice.application.dto.request.TenantOnboardingRequest;
import com.invoiceautomationservice.application.port.out.CompanyMemberRepository;
import com.invoiceautomationservice.application.port.out.CompanyRepository;
import com.invoiceautomationservice.domain.model.CompanyMember;
import com.invoiceautomationservice.domain.model.CompanyRole;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

class TenantOnboardingServiceTest {
  @Test
  void createsCompanyAndOwnerAtomically() {
    CompanyRepository companies = mock(CompanyRepository.class);
    CompanyMemberRepository members = mock(CompanyMemberRepository.class);
    PasswordEncoder encoder = mock(PasswordEncoder.class);
    AuditTrailService audit = mock(AuditTrailService.class);
    Clock clock = Clock.fixed(Instant.parse("2026-09-23T15:00:00Z"), ZoneOffset.UTC);
    TenantOnboardingService service = new TenantOnboardingService(
        companies, members, encoder, audit, clock);
    var request = new TenantOnboardingRequest("Owner", "owner", "OWNER@example.com",
        "secret123", new TenantOnboardingRequest.Company(
            "Company SAC", "Company", "20123456789", "Lima"));
    when(companies.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    when(encoder.encode("secret123")).thenReturn("hash");
    when(members.create(any(), any(), any(), any(), any(), any(), any()))
        .thenAnswer(invocation -> new CompanyMember("user-1", invocation.getArgument(0),
            "Owner", "owner", "owner@example.com", true, CompanyRole.OWNER,
            CompanyRole.OWNER.permissions(), invocation.getArgument(6)));

    var response = service.register(request);

    assertThat(response.company().getId()).isNotBlank();
    assertThat(response.owner().role()).isEqualTo(CompanyRole.OWNER);
    verify(members).create(response.company().getId(), "Owner", "owner",
        "owner@example.com", "hash", CompanyRole.OWNER,
        Instant.parse("2026-09-23T15:00:00Z"));
  }
}
