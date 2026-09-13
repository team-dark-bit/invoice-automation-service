package com.invoiceautomationservice.application.service.mapper;

import com.invoiceautomationservice.application.dto.response.CompanyResponse;
import com.invoiceautomationservice.domain.model.Company;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CompanyDomainResponseMapper {
  CompanyResponse toResponse(Company company);
}
