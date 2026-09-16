package com.invoiceautomationservice.infrastructure.adapter.out.persistence;

import com.invoiceautomationservice.application.port.out.CustomerRepository;
import com.invoiceautomationservice.domain.model.Customer;
import com.invoiceautomationservice.infrastructure.adapter.out.persistence.mapper.domain.CustomerDaoDomainMapper;
import com.invoiceautomationservice.infrastructure.config.exception.ApplicationException;
import com.invoiceautomationservice.infrastructure.adapter.out.persistence.repository.JpaCustomerRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import static com.invoiceautomationservice.infrastructure.config.exception.RuntimeErrors.CUSTOMER_NOT_FOUND;

@Repository
@RequiredArgsConstructor
public class CustomerPersistenceAdapter implements CustomerRepository {

  private final JpaCustomerRepository jpaCustomerRepository;
  private final CustomerDaoDomainMapper customerDaoDomainMapper;

  @Override
  public void save(Customer customer) {
    jpaCustomerRepository.save(customerDaoDomainMapper.toDao(customer));
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
}

