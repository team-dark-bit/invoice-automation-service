package com.invoiceautomationservice.application.service.mapper;

import com.invoiceautomationservice.domain.model.Customer;
import com.invoiceautomationservice.application.dto.request.CreateCustomerRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CustomerRequestDomainMapper {
    @Mapping(target = "id", ignore = true)
    Customer fromRequest(CreateCustomerRequest createCustomerRequest);
}

