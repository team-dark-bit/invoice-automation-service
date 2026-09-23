package com.invoiceautomationservice.application.port.out;

import java.util.List;
import com.invoiceautomationservice.domain.model.CompanyRole;
import java.util.Optional;

public interface UserCompanyRepository {
  void associate(String username, String companyId);
  boolean hasAccess(String username, String companyId);
  List<String> findCompanyIds(String username);
  Optional<CompanyRole> findRole(String username, String companyId);
}
