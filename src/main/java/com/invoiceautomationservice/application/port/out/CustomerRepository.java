package com.invoiceautomationservice.application.port.out;

import com.invoiceautomationservice.domain.model.Customer;
import java.util.List;

public interface CustomerRepository {
  void save(Customer customer);
  Customer findByIdAndCompanyId(String id, String companyId);
  List<Customer> findAllByCompanyIdAndActiveTrue(String companyId);
}

