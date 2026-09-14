package com.invoiceautomationservice.infrastructure.adapter.out.persistence.mapper.domain;

import com.invoiceautomationservice.domain.model.Company;
import com.invoiceautomationservice.infrastructure.adapter.out.persistence.entity.CompanyEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = IdMapper.class)
public interface CompanyDaoDomainMapper {
  Company toDomain(CompanyEntity dao);

  @Mapping(target = "id", source = "id", qualifiedByName = "generateId")
  CompanyEntity toDao(Company company);
}
