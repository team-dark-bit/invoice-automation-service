package com.invoiceautomationservice.application.port.in;

import com.invoiceautomationservice.application.dto.request.CreateCustomerRequest;
import com.invoiceautomationservice.application.dto.response.CustomerResponse;
import java.util.List;

public interface CustomerUseCase {
  void create(CreateCustomerRequest createCustomerRequest);
  CustomerResponse findById(String customerId);
  List<CustomerResponse> findAll();
}

