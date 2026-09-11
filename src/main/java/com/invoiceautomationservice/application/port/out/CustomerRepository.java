package com.invoiceautomationservice.application.port.out;

import com.invoiceautomationservice.domain.model.Customer;
import java.util.List;

public interface CustomerRepository {
  void save(Customer customer);
  Customer findById(String id);
  List<Customer> findAllByActiveTrue();
}

