package com.invoiceautomationservice.application.service.mapper;

import com.invoiceautomationservice.domain.model.Customer;
import com.invoiceautomationservice.application.dto.request.CreateCustomerRequest;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CustomerRequestDomainMapper {
    Customer fromRequest(CreateCustomerRequest createCustomerRequest);
}

