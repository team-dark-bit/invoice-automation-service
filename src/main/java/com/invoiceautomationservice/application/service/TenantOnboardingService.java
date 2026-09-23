package com.invoiceautomationservice.application.service;

import com.invoiceautomationservice.application.dto.request.TenantOnboardingRequest;
import com.invoiceautomationservice.application.dto.response.CompanyMemberResponse;
import com.invoiceautomationservice.application.dto.response.CompanyResponse;
import com.invoiceautomationservice.application.dto.response.TenantOnboardingResponse;
import com.invoiceautomationservice.application.port.out.CompanyMemberRepository;
import com.invoiceautomationservice.application.port.out.CompanyRepository;
import com.invoiceautomationservice.domain.model.AuditAction;
import com.invoiceautomationservice.domain.model.Company;
import com.invoiceautomationservice.domain.model.CompanyMember;
import com.invoiceautomationservice.domain.model.CompanyRole;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.invoiceautomationservice.infrastructure.config.exception.ApplicationException;
import static com.invoiceautomationservice.infrastructure.config.exception.RuntimeErrors.USER_IDENTITY_ALREADY_EXISTS;

@Service
@RequiredArgsConstructor
public class TenantOnboardingService {
  private final CompanyRepository companyRepository;
  private final CompanyMemberRepository memberRepository;
  private final PasswordEncoder passwordEncoder;
  private final AuditTrailService auditTrailService;
  private final Clock clock;

  @Transactional
  public TenantOnboardingResponse register(TenantOnboardingRequest request) {
    String username = request.username().strip();
    String email = request.email().strip().toLowerCase();
    if (memberRepository.userExists(username, email)) {
      throw new ApplicationException(USER_IDENTITY_ALREADY_EXISTS);
    }
    Company company = new Company();
    company.setId(UUID.randomUUID().toString());
    company.setLegalName(request.company().legalName().strip());
    company.setTradeName(stripNullable(request.company().tradeName()));
    company.setTaxId(request.company().taxId());
    company.setAddress(stripNullable(request.company().address()));
    company.setActive(true);
    Company savedCompany = companyRepository.save(company);
    CompanyMember owner = memberRepository.create(savedCompany.getId(), request.fullName().strip(),
        username, email, passwordEncoder.encode(request.password()), CompanyRole.OWNER,
        Instant.now(clock));
    auditTrailService.recordAs(username, savedCompany.getId(), AuditAction.TENANT_ONBOARDED,
        "COMPANY", savedCompany.getId(), "SUCCESS", "Owner and company registered");
    return new TenantOnboardingResponse(toCompanyResponse(savedCompany), toMemberResponse(owner),
        "Configure the issuer tax profile and document series, then authenticate at /api/auth/login");
  }

  private CompanyResponse toCompanyResponse(Company company) {
    CompanyResponse response = new CompanyResponse();
    response.setId(company.getId());
    response.setLegalName(company.getLegalName());
    response.setTradeName(company.getTradeName());
    response.setTaxId(company.getTaxId());
    response.setAddress(company.getAddress());
    response.setActive(company.isActive());
    return response;
  }

  private CompanyMemberResponse toMemberResponse(CompanyMember member) {
    return new CompanyMemberResponse(member.userId(), member.companyId(), member.fullName(),
        member.username(), member.email(), member.enabled(), member.role(), member.permissions(),
        member.joinedAt());
  }

  private String stripNullable(String value) {
    return value == null ? null : value.strip();
  }
}
