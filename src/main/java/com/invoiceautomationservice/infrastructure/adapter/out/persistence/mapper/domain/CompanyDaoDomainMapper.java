package com.invoiceautomationservice.infrastructure.adapter.out.persistence.mapper.domain;

import com.invoiceautomationservice.domain.model.Company;
import com.invoiceautomationservice.infrastructure.adapter.out.persistence.entity.CompanyDao;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = IdMapper.class)
public interface CompanyDaoDomainMapper {
  Company toDomain(CompanyDao dao);

  @Mapping(target = "id", source = "id", qualifiedByName = "generateId")
  CompanyDao toDao(Company company);
}
