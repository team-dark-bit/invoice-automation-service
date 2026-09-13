package com.invoiceautomationservice.application.port.out;

import com.invoiceautomationservice.domain.model.Company;
import java.util.List;

public interface CompanyRepository {
  Company save(Company company);
  Company findById(String id);
  List<Company> findAllByActiveTrue();
}
