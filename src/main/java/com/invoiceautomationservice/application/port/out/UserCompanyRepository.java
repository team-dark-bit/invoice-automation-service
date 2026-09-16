package com.invoiceautomationservice.application.port.out;

import java.util.List;

public interface UserCompanyRepository {
  void associate(String username, String companyId);
  boolean hasAccess(String username, String companyId);
  List<String> findCompanyIds(String username);
}
