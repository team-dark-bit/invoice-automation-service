package com.invoiceautomationservice.application.service;

import com.invoiceautomationservice.application.dto.request.CreateCompanyMemberRequest;
import com.invoiceautomationservice.application.dto.request.UpdateCompanyMemberRequest;
import com.invoiceautomationservice.application.dto.request.AddExistingCompanyMemberRequest;
import com.invoiceautomationservice.application.dto.response.CompanyMemberResponse;
import com.invoiceautomationservice.application.port.out.CompanyMemberRepository;
import com.invoiceautomationservice.domain.model.AuditAction;
import com.invoiceautomationservice.domain.model.CompanyMember;
import com.invoiceautomationservice.domain.model.CompanyPermission;
import com.invoiceautomationservice.domain.model.CompanyRole;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.invoiceautomationservice.infrastructure.config.exception.ApplicationException;
import static com.invoiceautomationservice.infrastructure.config.exception.RuntimeErrors.*;

@Service
@RequiredArgsConstructor
public class CompanyMemberService {
  private final CompanyMemberRepository repository;
  private final CompanyAccessService accessService;
  private final PasswordEncoder passwordEncoder;
  private final AuditTrailService auditTrailService;
  private final Clock clock;

  @Transactional
  public CompanyMemberResponse create(String companyId, CreateCompanyMemberRequest request) {
    accessService.requirePermission(companyId, CompanyPermission.MEMBER_MANAGE);
    if (request.role() == CompanyRole.OWNER
        && accessService.currentRole(companyId) != CompanyRole.OWNER) {
      throw new ApplicationException(OWNER_MANAGEMENT_FORBIDDEN);
    }
    if (repository.userExists(request.username().strip(), request.email().strip().toLowerCase())) {
      throw new ApplicationException(USER_IDENTITY_ALREADY_EXISTS);
    }
    CompanyMember member = repository.create(companyId, request.fullName().strip(),
        request.username().strip(), request.email().strip().toLowerCase(),
        passwordEncoder.encode(request.password()), request.role(), Instant.now(clock));
    auditTrailService.record(companyId, AuditAction.COMPANY_MEMBER_ADDED, "COMPANY_MEMBER",
        member.userId(), "SUCCESS", "Role: " + member.role());
    return toResponse(member);
  }

  @Transactional
  public CompanyMemberResponse addExisting(
      String companyId, AddExistingCompanyMemberRequest request) {
    accessService.requirePermission(companyId, CompanyPermission.MEMBER_MANAGE);
    if (request.role() == CompanyRole.OWNER
        && accessService.currentRole(companyId) != CompanyRole.OWNER) {
      throw new ApplicationException(OWNER_MANAGEMENT_FORBIDDEN);
    }
    CompanyMember member = repository.associateExisting(
        companyId, request.username().strip(), request.role(), Instant.now(clock));
    auditTrailService.record(companyId, AuditAction.COMPANY_MEMBER_ADDED, "COMPANY_MEMBER",
        member.userId(), "SUCCESS", "Existing user associated with role: " + member.role());
    return toResponse(member);
  }

  @Transactional(readOnly = true)
  public List<CompanyMemberResponse> findAll(String companyId) {
    accessService.requirePermission(companyId, CompanyPermission.MEMBER_MANAGE);
    return repository.findAll(companyId).stream().map(this::toResponse).toList();
  }

  @Transactional
  public CompanyMemberResponse update(
      String companyId, String userId, UpdateCompanyMemberRequest request) {
    accessService.requirePermission(companyId, CompanyPermission.MEMBER_MANAGE);
    CompanyMember current = repository.findByUserId(companyId, userId);
    if ((current.role() == CompanyRole.OWNER || request.role() == CompanyRole.OWNER)
        && accessService.currentRole(companyId) != CompanyRole.OWNER) {
      throw new ApplicationException(OWNER_MANAGEMENT_FORBIDDEN);
    }
    if (current.role() == CompanyRole.OWNER
        && ((request.role() != null && request.role() != CompanyRole.OWNER)
            || Boolean.FALSE.equals(request.enabled()))
        && repository.countOwners(companyId) <= 1) {
      throw new ApplicationException(LAST_COMPANY_OWNER);
    }
    CompanyMember updated = repository.update(companyId, userId, request.role(), request.enabled());
    auditTrailService.record(companyId, AuditAction.COMPANY_MEMBER_UPDATED, "COMPANY_MEMBER",
        userId, "SUCCESS", "Role: " + updated.role() + "; enabled: " + updated.enabled());
    return toResponse(updated);
  }

  @Transactional
  public void remove(String companyId, String userId) {
    accessService.requirePermission(companyId, CompanyPermission.MEMBER_MANAGE);
    CompanyMember member = repository.findByUserId(companyId, userId);
    if (member.role() == CompanyRole.OWNER
        && accessService.currentRole(companyId) != CompanyRole.OWNER) {
      throw new ApplicationException(OWNER_MANAGEMENT_FORBIDDEN);
    }
    if (member.role() == CompanyRole.OWNER && repository.countOwners(companyId) <= 1) {
      throw new ApplicationException(LAST_COMPANY_OWNER);
    }
    repository.remove(companyId, userId);
    auditTrailService.record(companyId, AuditAction.COMPANY_MEMBER_REMOVED, "COMPANY_MEMBER",
        userId, "SUCCESS", "Member access revoked");
  }

  private CompanyMemberResponse toResponse(CompanyMember member) {
    return new CompanyMemberResponse(member.userId(), member.companyId(), member.fullName(),
        member.username(), member.email(), member.enabled(), member.role(), member.permissions(),
        member.joinedAt());
  }
}
