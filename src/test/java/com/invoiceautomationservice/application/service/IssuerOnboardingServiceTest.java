package com.invoiceautomationservice.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.invoiceautomationservice.application.dto.request.ConfigureIssuerTaxProfileRequest;
import com.invoiceautomationservice.application.port.out.CompanyRepository;
import com.invoiceautomationservice.application.port.out.IssuerTaxProfileRepository;
import com.invoiceautomationservice.domain.model.Company;
import com.invoiceautomationservice.domain.model.IssuerTaxProfile;
import com.invoiceautomationservice.domain.model.TaxpayerType;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class IssuerOnboardingServiceTest {

  @Test
  void configuresTaxProfileForOwnedCompanyWithValidRuc() {
    CompanyRepository companyRepository = mock(CompanyRepository.class);
    IssuerTaxProfileRepository profileRepository = mock(IssuerTaxProfileRepository.class);
    CompanyAccessService accessService = mock(CompanyAccessService.class);
    Clock clock = Clock.fixed(Instant.parse("2026-09-20T10:00:00Z"), ZoneOffset.UTC);
    IssuerOnboardingService service = new IssuerOnboardingService(
        companyRepository, profileRepository, accessService, clock,
        mock(AuditTrailService.class));
    Company company = new Company();
    company.setId("company-1");
    company.setTaxId("20123456789");
    when(companyRepository.findById("company-1")).thenReturn(company);
    when(profileRepository.save(org.mockito.ArgumentMatchers.any(IssuerTaxProfile.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    ConfigureIssuerTaxProfileRequest request = new ConfigureIssuerTaxProfileRequest(
        TaxpayerType.LEGAL_ENTITY, "Av. Lima 123", "150101", "Lima", "Lima", "Lima", "PE");

    var response = service.configure("company-1", request);

    assertThat(response.completed()).isTrue();
    assertThat(response.ubigeo()).isEqualTo("150101");
    verify(accessService).requireAccess("company-1");
  }
}
