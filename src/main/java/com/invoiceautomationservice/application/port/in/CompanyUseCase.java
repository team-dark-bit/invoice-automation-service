package com.invoiceautomationservice.application.port.in;

import com.invoiceautomationservice.application.dto.request.CreateCompanyRequest;
import com.invoiceautomationservice.application.dto.response.CompanyResponse;
import java.util.List;

public interface CompanyUseCase {
  CompanyResponse create(CreateCompanyRequest request);
  CompanyResponse findById(String companyId);
  List<CompanyResponse> findAll();
}
