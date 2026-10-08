package com.invoiceautomationservice.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.invoiceautomationservice.application.port.out.CurrentUserProvider;
import com.invoiceautomationservice.application.port.out.UserCompanyRepository;
import com.invoiceautomationservice.application.port.out.UserAccountRepository;
import com.invoiceautomationservice.domain.model.UserAccount;
import com.invoiceautomationservice.infrastructure.config.exception.ApplicationException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CompanyAccessServiceTest {

  private CurrentUserProvider currentUserProvider;
  private UserCompanyRepository repository;
  private CompanyAccessService service;
  private UserAccountRepository userAccountRepository;

  @BeforeEach
  void setUp() {
    currentUserProvider = mock(CurrentUserProvider.class);
    repository = mock(UserCompanyRepository.class);
    userAccountRepository = mock(UserAccountRepository.class);
    service = new CompanyAccessService(currentUserProvider, repository, userAccountRepository);
    when(currentUserProvider.username()).thenReturn("owner");
  }

  @Test
  void associatesCompanyWithAuthenticatedUser() {
    service.associateCurrentUser("company-1");

    verify(repository).associate("owner", "company-1");
  }

  @Test
  void rejectsCompanyOwnedByAnotherTenantWithoutLeakingIt() {
    when(repository.hasAccess("owner", "company-2")).thenReturn(false);

    assertThatThrownBy(() -> service.requireAccess("company-2"))
        .isInstanceOf(ApplicationException.class)
        .hasMessage("The company with id: company-2 does not exist");
  }

  @Test
  void resolvesOnlyCompanyWithoutHeader() {
    when(repository.findCompanyIds("owner")).thenReturn(List.of("company-1"));

    assertThat(service.resolveCompanyId(null)).isEqualTo("company-1");
  }

  @Test
  void requiresHeaderWhenUserHasMultipleCompanies() {
    when(repository.findCompanyIds("owner")).thenReturn(List.of("company-1", "company-2"));
    when(userAccountRepository.findByUsername("owner")).thenReturn(
        new UserAccount("user-1", "Owner", "owner", "owner@example.com", true, null));

    assertThatThrownBy(() -> service.resolveCompanyId(null))
        .isInstanceOf(ApplicationException.class)
        .hasMessage("X-Company-Id is required when the user does not have exactly one company");
  }

  @Test
  void resolvesDefaultCompanyWhenUserHasMultipleCompanies() {
    when(repository.findCompanyIds("owner")).thenReturn(List.of("company-1", "company-2"));
    when(userAccountRepository.findByUsername("owner")).thenReturn(
        new UserAccount("user-1", "Owner", "owner", "owner@example.com", true, "company-2"));

    assertThat(service.resolveCompanyId(null)).isEqualTo("company-2");
  }

  @Test
  void validatesExplicitCompanyHeader() {
    when(repository.hasAccess("owner", "company-2")).thenReturn(true);

    assertThat(service.resolveCompanyId("company-2")).isEqualTo("company-2");
    verify(repository).hasAccess("owner", "company-2");
  }
}
