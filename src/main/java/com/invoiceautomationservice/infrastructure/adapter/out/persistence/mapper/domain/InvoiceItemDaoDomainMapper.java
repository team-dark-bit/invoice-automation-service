package com.invoiceautomationservice.infrastructure.adapter.out.persistence.mapper.domain;

import com.invoiceautomationservice.domain.model.InvoiceItem;
import com.invoiceautomationservice.infrastructure.adapter.out.persistence.entity.InvoiceItemDao;
import java.util.UUID;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface InvoiceItemDaoDomainMapper {
  InvoiceItem toDomain(InvoiceItemDao dao);

  @Mapping(target = "invoiceDraftId", source = "invoiceDraftId")
  @Mapping(target = "position", source = "position")
  InvoiceItemDao toDao(InvoiceItem item, UUID invoiceDraftId, int position);
}
