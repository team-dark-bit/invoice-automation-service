package com.invoiceautomationservice.application.port.in;

import com.invoiceautomationservice.application.dto.request.ConfigureIssuerTaxProfileRequest;
import com.invoiceautomationservice.application.dto.response.IssuerTaxProfileResponse;

public interface IssuerOnboardingUseCase {
  IssuerTaxProfileResponse configure(String companyId, ConfigureIssuerTaxProfileRequest request);
  IssuerTaxProfileResponse findByCompanyId(String companyId);
}
