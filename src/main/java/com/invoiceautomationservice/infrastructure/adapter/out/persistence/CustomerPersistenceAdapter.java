package com.invoiceautomationservice.infrastructure.adapter.out.persistence;

import com.invoiceautomationservice.application.port.out.CustomerRepository;
import com.invoiceautomationservice.domain.model.Customer;
import com.invoiceautomationservice.infrastructure.adapter.out.persistence.mapper.domain.CustomerDaoDomainMapper;
import com.invoiceautomationservice.infrastructure.config.exception.ApplicationException;
import com.invoiceautomationservice.infrastructure.adapter.out.persistence.repository.JpaCustomerRepository;
import java.util.List;
import com.invoiceautomationservice.application.model.PageQuery;
import com.invoiceautomationservice.application.model.PageResult;
import com.invoiceautomationservice.infrastructure.adapter.out.persistence.entity.CustomerEntity;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import static com.invoiceautomationservice.infrastructure.config.exception.RuntimeErrors.CUSTOMER_NOT_FOUND;

@Repository
@RequiredArgsConstructor
public class CustomerPersistenceAdapter implements CustomerRepository {

  private final JpaCustomerRepository jpaCustomerRepository;
  private final CustomerDaoDomainMapper customerDaoDomainMapper;

  @Override
  public Customer save(Customer customer) {
    return customerDaoDomainMapper.toDomain(
        jpaCustomerRepository.save(customerDaoDomainMapper.toDao(customer)));
  }

  @Override
  public Customer findByIdAndCompanyId(String id, String companyId) {
    return jpaCustomerRepository.findByIdAndCompanyId(id, companyId)
            .map(customerDaoDomainMapper::toDomain)
            .orElseThrow(() -> new ApplicationException(CUSTOMER_NOT_FOUND, id));
  }

  @Override
  public List<Customer> findAllByCompanyIdAndActiveTrue(String companyId) {
    return jpaCustomerRepository.findAllByCompanyIdAndActiveTrue(companyId)
            .stream()
            .map(customerDaoDomainMapper::toDomain)
            .toList();
  }

  @Override
  public java.util.Optional<Customer> findByCompanyIdAndDocumentTypeAndDocumentNumber(
      String companyId, String documentType, String documentNumber) {
    return jpaCustomerRepository
        .findByCompanyIdAndDocumentTypeAndDocumentNumber(
            companyId, documentType, documentNumber)
        .map(customerDaoDomainMapper::toDomain);
  }

  @Override
  public PageResult<Customer> search(
      String companyId, String queryText, Boolean active, PageQuery pageQuery) {
    Specification<CustomerEntity> specification =
        (root, query, cb) -> cb.equal(root.get("companyId"), companyId);
    if (active != null) specification = specification.and(
        (root, query, cb) -> cb.equal(root.get("active"), active));
    if (queryText != null && !queryText.isBlank()) {
      String value = "%" + queryText.strip().toLowerCase() + "%";
      specification = specification.and((root, query, cb) -> cb.or(
          cb.like(cb.lower(root.get("fullName")), value),
          cb.like(cb.lower(root.get("companyName")), value),
          cb.like(cb.lower(root.get("documentNumber")), value)));
    }
    var result = jpaCustomerRepository.findAll(specification, PageRequest.of(
        pageQuery.page(), pageQuery.size(), Sort.by("fullName").ascending()));
    return new PageResult<>(result.getContent().stream()
        .map(customerDaoDomainMapper::toDomain).toList(), result.getNumber(), result.getSize(),
        result.getTotalElements(), result.getTotalPages());
  }
}

