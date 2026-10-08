package com.invoiceautomationservice.application.port.in;

import com.invoiceautomationservice.application.dto.request.CreateCompanyRequest;
import com.invoiceautomationservice.application.dto.response.CompanyResponse;
import java.util.List;
import com.invoiceautomationservice.application.dto.response.PageResponse;
import com.invoiceautomationservice.application.dto.request.UpdateCompanyRequest;

public interface CompanyUseCase {
  CompanyResponse create(CreateCompanyRequest request);
  CompanyResponse findById(String companyId);
  List<CompanyResponse> findAll();
  PageResponse<CompanyResponse> search(String query, Boolean active, int page, int size);
  CompanyResponse update(String companyId, UpdateCompanyRequest request);
}
