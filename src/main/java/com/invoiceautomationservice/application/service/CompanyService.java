package com.invoiceautomationservice.application.service;

import com.invoiceautomationservice.application.dto.request.CreateCompanyRequest;
import com.invoiceautomationservice.application.dto.response.CompanyResponse;
import com.invoiceautomationservice.application.dto.response.PageResponse;
import com.invoiceautomationservice.application.model.PageQuery;
import com.invoiceautomationservice.application.port.in.CompanyUseCase;
import com.invoiceautomationservice.application.port.out.CompanyRepository;
import com.invoiceautomationservice.application.service.mapper.CompanyDomainResponseMapper;
import com.invoiceautomationservice.application.service.mapper.CompanyRequestDomainMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CompanyService implements CompanyUseCase {

  private final CompanyRepository companyRepository;
  private final CompanyRequestDomainMapper requestMapper;
  private final CompanyDomainResponseMapper responseMapper;
  private final CompanyAccessService companyAccessService;

  @Override
  @Transactional
  public CompanyResponse create(CreateCompanyRequest request) {
    var company = companyRepository.save(requestMapper.fromRequest(request));
    companyAccessService.associateCurrentUser(company.getId());
    return responseMapper.toResponse(company);
  }

  @Override
  public CompanyResponse findById(String companyId) {
    companyAccessService.requireAccess(companyId);
    return responseMapper.toResponse(companyRepository.findById(companyId));
  }

  @Override
  public List<CompanyResponse> findAll() {
    return companyRepository.findAllByIdInAndActiveTrue(companyAccessService.currentCompanyIds()).stream()
            .map(responseMapper::toResponse)
            .toList();
  }

  @Override
  public PageResponse<CompanyResponse> search(String query, Boolean active, int page, int size) {
    return companyRepository.search(
        companyAccessService.currentCompanyIds(), query, active, new PageQuery(page, size))
        .map(responseMapper::toResponse);
  }
}
