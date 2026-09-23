package com.invoiceautomationservice.application.port.in;

import com.invoiceautomationservice.application.dto.request.CreateCustomerRequest;
import com.invoiceautomationservice.application.dto.response.CustomerResponse;
import java.util.List;
import com.invoiceautomationservice.application.dto.response.PageResponse;

public interface CustomerUseCase {
  void create(CreateCustomerRequest createCustomerRequest, String requestedCompanyId);
  CustomerResponse findById(String customerId, String requestedCompanyId);
  List<CustomerResponse> findAll(String requestedCompanyId);
  PageResponse<CustomerResponse> search(
      String requestedCompanyId, String query, Boolean active, int page, int size);
}

