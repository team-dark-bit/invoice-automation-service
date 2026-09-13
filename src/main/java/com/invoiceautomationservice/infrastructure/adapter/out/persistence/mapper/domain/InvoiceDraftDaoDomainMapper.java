package com.invoiceautomationservice.infrastructure.adapter.out.persistence.mapper.domain;

import com.invoiceautomationservice.domain.model.InvoiceDraft;
import com.invoiceautomationservice.infrastructure.adapter.out.persistence.entity.InvoiceDraftDao;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface InvoiceDraftDaoDomainMapper {
  InvoiceDraftDao toDao(InvoiceDraft domain);
}
