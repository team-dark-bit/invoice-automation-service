package com.invoiceautomationservice.application.service;

import com.invoiceautomationservice.application.dto.request.ConfigureIssuerTaxProfileRequest;
import com.invoiceautomationservice.application.dto.response.IssuerTaxProfileResponse;
import com.invoiceautomationservice.application.port.in.IssuerOnboardingUseCase;
import com.invoiceautomationservice.application.port.out.CompanyRepository;
import com.invoiceautomationservice.application.port.out.IssuerTaxProfileRepository;
import com.invoiceautomationservice.domain.model.Company;
import com.invoiceautomationservice.domain.model.IssuerTaxProfile;
import com.invoiceautomationservice.domain.model.AuditAction;
import com.invoiceautomationservice.domain.model.CompanyPermission;
import com.invoiceautomationservice.infrastructure.config.exception.ApplicationException;
import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.invoiceautomationservice.infrastructure.config.exception.RuntimeErrors.INVALID_ISSUER_RUC;

@Service
@RequiredArgsConstructor
public class IssuerOnboardingService implements IssuerOnboardingUseCase {

  private final CompanyRepository companyRepository;
  private final IssuerTaxProfileRepository profileRepository;
  private final CompanyAccessService companyAccessService;
  private final Clock clock;
  private final AuditTrailService auditTrailService;

  @Override
  @Transactional
  public IssuerTaxProfileResponse configure(
      String companyId, ConfigureIssuerTaxProfileRequest request) {
    companyAccessService.requireAccess(companyId);
    companyAccessService.requirePermission(companyId, CompanyPermission.ONBOARDING_MANAGE);
    Company company = companyRepository.findById(companyId);
    if (company.getTaxId() == null || !company.getTaxId().matches("\\d{11}")) {
      throw new ApplicationException(INVALID_ISSUER_RUC, companyId);
    }
    Instant now = Instant.now(clock);
    Instant createdAt = profileRepository.existsByCompanyId(companyId)
        ? profileRepository.findByCompanyId(companyId).createdAt()
        : now;
    IssuerTaxProfile profile = new IssuerTaxProfile(
        companyId, request.taxpayerType(), request.fiscalAddress(), request.ubigeo(),
        request.department(), request.province(), request.district(), request.countryCode(),
        createdAt, now);
    IssuerTaxProfile saved = profileRepository.save(profile);
    auditTrailService.record(companyId, AuditAction.ISSUER_TAX_PROFILE_CONFIGURED,
        "ISSUER_TAX_PROFILE", companyId, "SUCCESS", "Issuer tax profile configured");
    return toResponse(saved);
  }

  @Override
  @Transactional(readOnly = true)
  public IssuerTaxProfileResponse findByCompanyId(String companyId) {
    companyAccessService.requireAccess(companyId);
    return toResponse(profileRepository.findByCompanyId(companyId));
  }

  private IssuerTaxProfileResponse toResponse(IssuerTaxProfile profile) {
    return new IssuerTaxProfileResponse(
        profile.companyId(), profile.taxpayerType(), profile.fiscalAddress(), profile.ubigeo(),
        profile.department(), profile.province(), profile.district(), profile.countryCode(), true,
        profile.createdAt(), profile.updatedAt());
  }
}
