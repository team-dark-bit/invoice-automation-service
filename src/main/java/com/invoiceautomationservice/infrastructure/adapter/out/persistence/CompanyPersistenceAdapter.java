package com.invoiceautomationservice.infrastructure.adapter.out.persistence;

import static com.invoiceautomationservice.infrastructure.config.exception.RuntimeErrors.COMPANY_NOT_FOUND;

import com.invoiceautomationservice.application.port.out.CompanyRepository;
import com.invoiceautomationservice.domain.model.Company;
import com.invoiceautomationservice.infrastructure.adapter.out.persistence.mapper.domain.CompanyDaoDomainMapper;
import com.invoiceautomationservice.infrastructure.adapter.out.persistence.repository.JpaCompanyRepository;
import com.invoiceautomationservice.infrastructure.config.exception.ApplicationException;
import java.util.List;
import com.invoiceautomationservice.application.model.PageQuery;
import com.invoiceautomationservice.application.model.PageResult;
import com.invoiceautomationservice.infrastructure.adapter.out.persistence.entity.CompanyEntity;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
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

  @Override
  public PageResult<Company> search(
      List<String> ids, String queryText, Boolean active, PageQuery pageQuery) {
    Specification<CompanyEntity> specification = (root, query, cb) -> root.get("id").in(ids);
    if (active != null) specification = specification.and(
        (root, query, cb) -> cb.equal(root.get("active"), active));
    if (queryText != null && !queryText.isBlank()) {
      String value = "%" + queryText.strip().toLowerCase() + "%";
      specification = specification.and((root, query, cb) -> cb.or(
          cb.like(cb.lower(root.get("legalName")), value),
          cb.like(cb.lower(root.get("tradeName")), value),
          cb.like(cb.lower(root.get("taxId")), value)));
    }
    var result = jpaCompanyRepository.findAll(specification, PageRequest.of(
        pageQuery.page(), pageQuery.size(), Sort.by("legalName").ascending()));
    return new PageResult<>(result.getContent().stream().map(mapper::toDomain).toList(),
        result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
  }
}
