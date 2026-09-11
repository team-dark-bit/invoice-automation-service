package com.invoiceautomationservice.infrastructure.adapter.out.persistence.mapper.domain;

import com.invoiceautomationservice.commons.util.IdGenerator;
import org.mapstruct.Named;
import org.springframework.stereotype.Component;

@Component
public class IdMapper {

    @Named("generateId")
    public String generateId(String id) {
        return (id == null || id.isBlank()) ? IdGenerator.generateId() : id;
    }
}