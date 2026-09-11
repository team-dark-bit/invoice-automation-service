package com.invoiceautomationservice.infrastructure.adapter.out.persistence.mapper.dto;

import com.invoiceautomationservice.application.dto.response.CustomerResponse;
import com.invoiceautomationservice.domain.model.Customer;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CustomerDomainResponseMapper {
    CustomerResponse toResponse(Customer customer);
}

