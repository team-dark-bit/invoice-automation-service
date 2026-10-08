package com.invoiceautomationservice.application.service;

import com.invoiceautomationservice.application.dto.response.UserSessionResponse;
import com.invoiceautomationservice.application.port.out.CompanyRepository;
import com.invoiceautomationservice.application.port.out.CurrentUserProvider;
import com.invoiceautomationservice.application.port.out.UserAccountRepository;
import com.invoiceautomationservice.application.port.out.UserCompanyRepository;
import com.invoiceautomationservice.domain.model.Company;
import com.invoiceautomationservice.domain.model.UserAccount;
import com.invoiceautomationservice.infrastructure.config.exception.ApplicationException;
import static com.invoiceautomationservice.infrastructure.config.exception.RuntimeErrors.COMPANY_NOT_FOUND;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserSessionService {
  private final CurrentUserProvider currentUserProvider;
  private final UserAccountRepository userAccountRepository;
  private final UserCompanyRepository userCompanyRepository;
  private final CompanyRepository companyRepository;

  @Transactional(readOnly = true)
  public UserSessionResponse current() {
    String username = currentUserProvider.username();
    UserAccount account = userAccountRepository.findByUsername(username);
    var memberships = userCompanyRepository.findAccesses(username);
    var companies = companyRepository.findAllByIdIn(
            memberships.stream().map(access -> access.companyId()).toList()).stream()
        .collect(Collectors.toMap(Company::getId, Function.identity()));
    var accessResponses = memberships.stream()
        .filter(access -> companies.containsKey(access.companyId()))
        .map(access -> {
          Company company = companies.get(access.companyId());
          return new UserSessionResponse.CompanyAccess(
              company.getId(), company.getLegalName(), company.getTradeName(), company.getTaxId(),
              company.isActive(), access.role(), access.role().permissions());
        }).toList();
    String defaultCompanyId = memberships.stream()
        .anyMatch(access -> access.companyId().equals(account.defaultCompanyId()))
        ? account.defaultCompanyId()
        : memberships.stream().map(access -> access.companyId()).findFirst().orElse(null);
    return new UserSessionResponse(account.id(), account.fullName(), account.username(),
        account.email(), defaultCompanyId, accessResponses);
  }

  @Transactional
  public UserSessionResponse changeDefaultCompany(String companyId) {
    String username = currentUserProvider.username();
    if (!userCompanyRepository.hasAccess(username, companyId)) {
      throw new ApplicationException(COMPANY_NOT_FOUND, companyId);
    }
    userAccountRepository.updateDefaultCompany(username, companyId);
    return current();
  }
}
