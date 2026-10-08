package com.invoiceautomationservice.infrastructure.adapter.out.persistence;

import static com.invoiceautomationservice.infrastructure.config.exception.RuntimeErrors.USER_NOT_FOUND;

import com.invoiceautomationservice.application.port.out.UserAccountRepository;
import com.invoiceautomationservice.domain.model.UserAccount;
import com.invoiceautomationservice.infrastructure.adapter.out.persistence.entity.UserEntity;
import com.invoiceautomationservice.infrastructure.adapter.out.persistence.repository.JpaUserRepository;
import com.invoiceautomationservice.infrastructure.config.exception.ApplicationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class UserAccountPersistenceAdapter implements UserAccountRepository {
  private final JpaUserRepository repository;

  @Override
  public UserAccount findByUsername(String username) {
    UserEntity user = repository.findByUsername(username)
        .orElseThrow(() -> new ApplicationException(USER_NOT_FOUND, username));
    return new UserAccount(user.getId(), user.getFullName(), user.getUsername(), user.getEmail(),
        user.isEnabled(), user.getDefaultCompanyId());
  }

  @Override
  public void updateDefaultCompany(String username, String companyId) {
    UserEntity user = repository.findByUsername(username)
        .orElseThrow(() -> new ApplicationException(USER_NOT_FOUND, username));
    user.setDefaultCompanyId(companyId);
    repository.save(user);
  }
}
