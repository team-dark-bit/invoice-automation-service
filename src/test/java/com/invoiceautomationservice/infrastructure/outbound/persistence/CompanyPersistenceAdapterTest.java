package com.invoiceautomationservice.infrastructure.outbound.persistence;

import static com.invoiceautomationservice.infrastructure.config.exception.RuntimeErrors.COMPANY_NOT_FOUND;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.invoiceautomationservice.domain.model.Company;
import com.invoiceautomationservice.infrastructure.adapter.out.persistence.CompanyPersistenceAdapter;
import com.invoiceautomationservice.infrastructure.adapter.out.persistence.entity.CompanyEntity;
import com.invoiceautomationservice.infrastructure.adapter.out.persistence.mapper.domain.CompanyDaoDomainMapper;
import com.invoiceautomationservice.infrastructure.adapter.out.persistence.repository.JpaCompanyRepository;
import com.invoiceautomationservice.infrastructure.config.exception.ApplicationException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CompanyPersistenceAdapterTest {

  private JpaCompanyRepository repository;
  private CompanyDaoDomainMapper mapper;
  private CompanyPersistenceAdapter adapter;

  @BeforeEach
  void setUp() {
    repository = mock(JpaCompanyRepository.class);
    mapper = mock(CompanyDaoDomainMapper.class);
    adapter = new CompanyPersistenceAdapter(repository, mapper);
  }

  @Test
  void savesCompany() {
    Company company = company();
    CompanyEntity dao = dao();
    when(mapper.toDao(company)).thenReturn(dao);
    when(repository.save(dao)).thenReturn(dao);
    when(mapper.toDomain(dao)).thenReturn(company);

    assertThat(adapter.save(company)).isSameAs(company);
    verify(repository).save(dao);
  }

  @Test
  void findsCompanyById() {
    Company company = company();
    CompanyEntity dao = dao();
    when(repository.findById("company-1")).thenReturn(Optional.of(dao));
    when(mapper.toDomain(dao)).thenReturn(company);

    assertThat(adapter.findById("company-1")).isSameAs(company);
  }

  @Test
  void throwsWhenCompanyDoesNotExist() {
    when(repository.findById("missing")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> adapter.findById("missing"))
            .isInstanceOf(ApplicationException.class)
            .hasMessage(COMPANY_NOT_FOUND.getMessage(), "missing");
  }

  @Test
  void listsActiveCompanies() {
    Company company = company();
    CompanyEntity dao = dao();
    when(repository.findAllByIdInAndActiveTrue(List.of("company-1"))).thenReturn(List.of(dao));
    when(mapper.toDomain(dao)).thenReturn(company);

    assertThat(adapter.findAllByIdInAndActiveTrue(List.of("company-1"))).containsExactly(company);
  }

  private Company company() {
    Company company = new Company();
    company.setId("company-1");
    company.setLegalName("Dark Bit SAC");
    company.setTaxId("20123456789");
    company.setActive(true);
    return company;
  }

  private CompanyEntity dao() {
    CompanyEntity dao = new CompanyEntity();
    dao.setId("company-1");
    dao.setLegalName("Dark Bit SAC");
    dao.setTaxId("20123456789");
    dao.setActive(true);
    return dao;
  }
}
