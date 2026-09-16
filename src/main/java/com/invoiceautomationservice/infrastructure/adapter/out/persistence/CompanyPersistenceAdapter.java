package com.invoiceautomationservice.infrastructure.adapter.out.persistence;

import static com.invoiceautomationservice.infrastructure.config.exception.RuntimeErrors.COMPANY_NOT_FOUND;

import com.invoiceautomationservice.application.port.out.CompanyRepository;
import com.invoiceautomationservice.domain.model.Company;
import com.invoiceautomationservice.infrastructure.adapter.out.persistence.mapper.domain.CompanyDaoDomainMapper;
import com.invoiceautomationservice.infrastructure.adapter.out.persistence.repository.JpaCompanyRepository;
import com.invoiceautomationservice.infrastructure.config.exception.ApplicationException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class CompanyPersistenceAdapter implements CompanyRepository {

  private final JpaCompanyRepository jpaCompanyRepository;
  private final CompanyDaoDomainMapper mapper;

  @Override
  public Company save(Company company) {
    return mapper.toDomain(jpaCompanyRepository.save(mapper.toDao(company)));
  }

  @Override
  public Company findById(String id) {
    return jpaCompanyRepository.findById(id)
            .map(mapper::toDomain)
            .orElseThrow(() -> new ApplicationException(COMPANY_NOT_FOUND, id));
  }

  @Override
  public List<Company> findAllByIdInAndActiveTrue(List<String> ids) {
    return jpaCompanyRepository.findAllByIdInAndActiveTrue(ids).stream()
            .map(mapper::toDomain)
            .toList();
  }
}
