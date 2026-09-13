package com.invoiceautomationservice.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.invoiceautomationservice.application.dto.request.CreateCompanyRequest;
import com.invoiceautomationservice.application.dto.response.CompanyResponse;
import com.invoiceautomationservice.application.port.out.CompanyRepository;
import com.invoiceautomationservice.application.service.mapper.CompanyDomainResponseMapper;
import com.invoiceautomationservice.application.service.mapper.CompanyRequestDomainMapper;
import com.invoiceautomationservice.domain.model.Company;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CompanyServiceTest {

  private CompanyRepository repository;
  private CompanyRequestDomainMapper requestMapper;
  private CompanyDomainResponseMapper responseMapper;
  private CompanyService service;

  @BeforeEach
  void setUp() {
    repository = mock(CompanyRepository.class);
    requestMapper = mock(CompanyRequestDomainMapper.class);
    responseMapper = mock(CompanyDomainResponseMapper.class);
    service = new CompanyService(repository, requestMapper, responseMapper);
  }

  @Test
  void createsCompany() {
    CreateCompanyRequest request = new CreateCompanyRequest();
    request.setLegalName("Dark Bit SAC");
    request.setTaxId("20123456789");
    Company company = company("company-1", "Dark Bit SAC");
    CompanyResponse response = response("company-1", "Dark Bit SAC");
    when(requestMapper.fromRequest(request)).thenReturn(company);
    when(repository.save(company)).thenReturn(company);
    when(responseMapper.toResponse(company)).thenReturn(response);

    assertThat(service.create(request)).isSameAs(response);
    verify(repository).save(company);
  }

  @Test
  void findsCompanyById() {
    Company company = company("company-1", "Dark Bit SAC");
    CompanyResponse response = response("company-1", "Dark Bit SAC");
    when(repository.findById("company-1")).thenReturn(company);
    when(responseMapper.toResponse(company)).thenReturn(response);

    assertThat(service.findById("company-1")).isSameAs(response);
  }

  @Test
  void listsOnlyActiveCompaniesProvidedByRepository() {
    Company first = company("company-1", "Dark Bit SAC");
    Company second = company("company-2", "Invoice SAC");
    CompanyResponse firstResponse = response("company-1", "Dark Bit SAC");
    CompanyResponse secondResponse = response("company-2", "Invoice SAC");
    when(repository.findAllByActiveTrue()).thenReturn(List.of(first, second));
    when(responseMapper.toResponse(first)).thenReturn(firstResponse);
    when(responseMapper.toResponse(second)).thenReturn(secondResponse);

    assertThat(service.findAll()).containsExactly(firstResponse, secondResponse);
  }

  private Company company(String id, String legalName) {
    Company company = new Company();
    company.setId(id);
    company.setLegalName(legalName);
    company.setTaxId("20123456789");
    company.setActive(true);
    return company;
  }

  private CompanyResponse response(String id, String legalName) {
    CompanyResponse response = new CompanyResponse();
    response.setId(id);
    response.setLegalName(legalName);
    response.setTaxId("20123456789");
    response.setActive(true);
    return response;
  }
}
