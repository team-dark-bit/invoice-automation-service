package com.invoiceautomationservice.application.port.out;

import com.invoiceautomationservice.domain.model.Customer;
import java.util.List;
import java.util.Optional;
import com.invoiceautomationservice.application.model.PageQuery;
import com.invoiceautomationservice.application.model.PageResult;

public interface CustomerRepository {
  Customer save(Customer customer);
  Customer findByIdAndCompanyId(String id, String companyId);
  List<Customer> findAllByCompanyIdAndActiveTrue(String companyId);
  Optional<Customer> findByCompanyIdAndDocumentTypeAndDocumentNumber(
      String companyId, String documentType, String documentNumber);
  PageResult<Customer> search(String companyId, String query, Boolean active, PageQuery page);
}

