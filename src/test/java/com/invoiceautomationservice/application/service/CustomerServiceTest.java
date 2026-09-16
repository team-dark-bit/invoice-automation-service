package com.invoiceautomationservice.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.invoiceautomationservice.application.dto.request.CreateCustomerRequest;
import com.invoiceautomationservice.application.dto.response.CustomerResponse;
import com.invoiceautomationservice.application.port.out.CustomerRepository;
import com.invoiceautomationservice.application.service.mapper.CustomerRequestDomainMapper;
import com.invoiceautomationservice.domain.model.Customer;
import com.invoiceautomationservice.infrastructure.adapter.out.persistence.mapper.dto.CustomerDomainResponseMapper;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CustomerServiceTest {

  private CustomerRepository repository;
  private CustomerRequestDomainMapper requestMapper;
  private CustomerDomainResponseMapper responseMapper;
  private CompanyAccessService accessService;
  private CustomerService service;

  @BeforeEach
  void setUp() {
    repository = mock(CustomerRepository.class);
    requestMapper = mock(CustomerRequestDomainMapper.class);
    responseMapper = mock(CustomerDomainResponseMapper.class);
    accessService = mock(CompanyAccessService.class);
    service = new CustomerService(repository, requestMapper, responseMapper, accessService);
  }

  @Test
  void createsCustomerInsideResolvedCompany() {
    CreateCustomerRequest request = new CreateCustomerRequest();
    Customer customer = new Customer();
    when(accessService.resolveCompanyId("company-1")).thenReturn("company-1");
    when(requestMapper.fromRequest(request)).thenReturn(customer);

    service.create(request, "company-1");

    assertThat(customer.getCompanyId()).isEqualTo("company-1");
    verify(repository).save(customer);
  }

  @Test
  void readsOnlyCustomersFromResolvedCompany() {
    Customer customer = new Customer();
    CustomerResponse response = new CustomerResponse();
    when(accessService.resolveCompanyId("company-1")).thenReturn("company-1");
    when(repository.findAllByCompanyIdAndActiveTrue("company-1"))
        .thenReturn(List.of(customer));
    when(responseMapper.toResponse(customer)).thenReturn(response);

    assertThat(service.findAll("company-1")).containsExactly(response);
  }
}
