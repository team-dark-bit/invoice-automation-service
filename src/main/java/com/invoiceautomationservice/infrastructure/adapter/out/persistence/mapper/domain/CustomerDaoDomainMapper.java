package com.invoiceautomationservice.infrastructure.adapter.out.persistence.mapper.domain;

import com.invoiceautomationservice.domain.model.Customer;
import com.invoiceautomationservice.infrastructure.adapter.out.persistence.entity.CustomerEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {IdMapper.class})
public interface CustomerDaoDomainMapper {

  Customer toDomain(CustomerEntity dao);

  @Mapping(target = "id", source = "id", qualifiedByName = "generateId")
  CustomerEntity toDao(Customer domain);
}
