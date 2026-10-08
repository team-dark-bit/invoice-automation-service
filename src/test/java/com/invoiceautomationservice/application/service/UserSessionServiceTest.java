package com.invoiceautomationservice.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.invoiceautomationservice.application.port.out.CompanyRepository;
import com.invoiceautomationservice.application.port.out.CurrentUserProvider;
import com.invoiceautomationservice.application.port.out.UserAccountRepository;
import com.invoiceautomationservice.application.port.out.UserCompanyRepository;
import com.invoiceautomationservice.domain.model.Company;
import com.invoiceautomationservice.domain.model.CompanyRole;
import com.invoiceautomationservice.domain.model.UserAccount;
import com.invoiceautomationservice.domain.model.UserCompanyAccess;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UserSessionServiceTest {
  private CurrentUserProvider currentUser;
  private UserAccountRepository users;
  private UserCompanyRepository memberships;
  private CompanyRepository companies;
  private UserSessionService service;

  @BeforeEach
  void setUp() {
    currentUser = mock(CurrentUserProvider.class);
    users = mock(UserAccountRepository.class);
    memberships = mock(UserCompanyRepository.class);
    companies = mock(CompanyRepository.class);
    service = new UserSessionService(currentUser, users, memberships, companies);
    when(currentUser.username()).thenReturn("owner");
    when(users.findByUsername("owner")).thenReturn(
        new UserAccount("user-1", "Owner", "owner", "owner@example.com", true, "company-2"));
    when(memberships.findAccesses("owner")).thenReturn(List.of(
        new UserCompanyAccess("company-1", CompanyRole.VIEWER),
        new UserCompanyAccess("company-2", CompanyRole.OWNER)));
    when(companies.findAllByIdIn(List.of("company-1", "company-2")))
        .thenReturn(List.of(company("company-1"), company("company-2")));
  }

  @Test
  void returnsIdentityCompaniesRolesAndDefaultCompany() {
    var response = service.current();

    assertThat(response.userId()).isEqualTo("user-1");
    assertThat(response.defaultCompanyId()).isEqualTo("company-2");
    assertThat(response.companies()).hasSize(2);
    assertThat(response.companies().get(1).role()).isEqualTo(CompanyRole.OWNER);
    assertThat(response.companies().get(1).permissions())
        .contains(com.invoiceautomationservice.domain.model.CompanyPermission.DOCUMENT_ISSUE);
  }

  @Test
  void changesOnlyToAnAccessibleCompany() {
    when(memberships.hasAccess("owner", "company-1")).thenReturn(true);

    var response = service.changeDefaultCompany("company-1");

    verify(users).updateDefaultCompany("owner", "company-1");
    assertThat(response.companies()).hasSize(2);
  }

  private Company company(String id) {
    Company company = new Company();
    company.setId(id);
    company.setLegalName("Company " + id);
    company.setTaxId("20123456789");
    company.setActive(true);
    return company;
  }
}
