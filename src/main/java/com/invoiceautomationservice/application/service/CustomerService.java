package com.invoiceautomationservice.application.service;

import com.invoiceautomationservice.application.service.mapper.CustomerRequestDomainMapper;
import com.invoiceautomationservice.application.dto.request.CreateCustomerRequest;
import com.invoiceautomationservice.application.dto.response.CustomerResponse;
import com.invoiceautomationservice.application.port.in.CustomerUseCase;
import com.invoiceautomationservice.application.port.out.CustomerRepository;
import com.invoiceautomationservice.infrastructure.adapter.out.persistence.mapper.dto.CustomerDomainResponseMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomerService implements CustomerUseCase {

  private final CustomerRepository customerRepository;
  private final CustomerRequestDomainMapper domainRequestMapper;
  private final CustomerDomainResponseMapper domainResponseMapper;

  @Override
  public void create(CreateCustomerRequest createCustomerRequest) {
    customerRepository.save(domainRequestMapper.fromRequest(createCustomerRequest));
  }

  @Override
  public CustomerResponse findById(String customerId) {
    return domainResponseMapper.toResponse(customerRepository.findById(customerId));
  }

  @Override
  public List<CustomerResponse> findAll() {
    return customerRepository.findAllByActiveTrue()
            .stream()
            .map(domainResponseMapper::toResponse)
            .toList();
  }
}

