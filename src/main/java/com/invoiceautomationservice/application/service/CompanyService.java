package com.invoiceautomationservice.application.service;

import com.invoiceautomationservice.application.dto.request.CreateCompanyRequest;
import com.invoiceautomationservice.application.dto.response.CompanyResponse;
import com.invoiceautomationservice.application.port.in.CompanyUseCase;
import com.invoiceautomationservice.application.port.out.CompanyRepository;
import com.invoiceautomationservice.application.service.mapper.CompanyDomainResponseMapper;
import com.invoiceautomationservice.application.service.mapper.CompanyRequestDomainMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CompanyService implements CompanyUseCase {

  private final CompanyRepository companyRepository;
  private final CompanyRequestDomainMapper requestMapper;
  private final CompanyDomainResponseMapper responseMapper;

  @Override
  public CompanyResponse create(CreateCompanyRequest request) {
    return responseMapper.toResponse(companyRepository.save(requestMapper.fromRequest(request)));
  }

  @Override
  public CompanyResponse findById(String companyId) {
    return responseMapper.toResponse(companyRepository.findById(companyId));
  }

  @Override
  public List<CompanyResponse> findAll() {
    return companyRepository.findAllByActiveTrue().stream()
            .map(responseMapper::toResponse)
            .toList();
  }
}
