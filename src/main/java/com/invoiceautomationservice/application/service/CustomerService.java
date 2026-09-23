package com.invoiceautomationservice.application.service;

import com.invoiceautomationservice.application.service.mapper.CustomerRequestDomainMapper;
import com.invoiceautomationservice.application.dto.request.CreateCustomerRequest;
import com.invoiceautomationservice.application.dto.response.CustomerResponse;
import com.invoiceautomationservice.application.dto.response.PageResponse;
import com.invoiceautomationservice.application.model.PageQuery;
import com.invoiceautomationservice.application.port.in.CustomerUseCase;
import com.invoiceautomationservice.application.port.out.CustomerRepository;
import com.invoiceautomationservice.infrastructure.adapter.out.persistence.mapper.dto.CustomerDomainResponseMapper;
import java.util.List;
import com.invoiceautomationservice.domain.model.AuditAction;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomerService implements CustomerUseCase {

  private final CustomerRepository customerRepository;
  private final CustomerRequestDomainMapper domainRequestMapper;
  private final CustomerDomainResponseMapper domainResponseMapper;
  private final CompanyAccessService companyAccessService;
  private final AuditTrailService auditTrailService;

  @Override
  public void create(CreateCustomerRequest createCustomerRequest, String requestedCompanyId) {
    String companyId = companyAccessService.resolveCompanyId(requestedCompanyId);
    var customer = domainRequestMapper.fromRequest(createCustomerRequest);
    customer.setCompanyId(companyId);
    customerRepository.save(customer);
    auditTrailService.record(companyId, AuditAction.CUSTOMER_CREATED, "CUSTOMER",
        customer.getId(), "SUCCESS", "Customer registered");
  }

  @Override
  public CustomerResponse findById(String customerId, String requestedCompanyId) {
    String companyId = companyAccessService.resolveCompanyId(requestedCompanyId);
    return domainResponseMapper.toResponse(customerRepository.findByIdAndCompanyId(customerId, companyId));
  }

  @Override
  public List<CustomerResponse> findAll(String requestedCompanyId) {
    String companyId = companyAccessService.resolveCompanyId(requestedCompanyId);
    return customerRepository.findAllByCompanyIdAndActiveTrue(companyId)
            .stream()
            .map(domainResponseMapper::toResponse)
            .toList();
  }

  @Override
  public PageResponse<CustomerResponse> search(
      String requestedCompanyId, String query, Boolean active, int page, int size) {
    String companyId = companyAccessService.resolveCompanyId(requestedCompanyId);
    return customerRepository.search(companyId, query, active, new PageQuery(page, size))
        .map(domainResponseMapper::toResponse);
  }
}

