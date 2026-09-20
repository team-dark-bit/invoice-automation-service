package com.invoiceautomationservice.application.port.out;

import com.invoiceautomationservice.domain.model.IssuerTaxProfile;

public interface IssuerTaxProfileRepository {
  IssuerTaxProfile save(IssuerTaxProfile profile);
  IssuerTaxProfile findByCompanyId(String companyId);
  boolean existsByCompanyId(String companyId);
}
