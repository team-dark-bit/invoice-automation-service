package com.invoiceautomationservice.application.service.mapper;

import com.invoiceautomationservice.application.dto.request.CreateCompanyRequest;
import com.invoiceautomationservice.domain.model.Company;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CompanyRequestDomainMapper {

  @Mapping(target = "id", ignore = true)
  Company fromRequest(CreateCompanyRequest request);
}
