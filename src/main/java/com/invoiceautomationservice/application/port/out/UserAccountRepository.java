package com.invoiceautomationservice.application.port.out;

import com.invoiceautomationservice.domain.model.UserAccount;

public interface UserAccountRepository {
  UserAccount findByUsername(String username);
  void updateDefaultCompany(String username, String companyId);
}
