package com.invoiceautomationservice.application.port.out;

import com.invoiceautomationservice.domain.model.Company;
import java.util.List;
import com.invoiceautomationservice.application.model.PageQuery;
import com.invoiceautomationservice.application.model.PageResult;

public interface CompanyRepository {
  Company save(Company company);
  Company findById(String id);
  List<Company> findAllByIdInAndActiveTrue(List<String> ids);
  PageResult<Company> search(List<String> ids, String query, Boolean active, PageQuery page);
}
