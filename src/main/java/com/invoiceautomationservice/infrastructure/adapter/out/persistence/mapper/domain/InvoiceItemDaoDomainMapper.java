package com.invoiceautomationservice.infrastructure.adapter.out.persistence.mapper.domain;

import com.invoiceautomationservice.domain.model.InvoiceItem;
import com.invoiceautomationservice.infrastructure.adapter.out.persistence.entity.InvoiceItemEntity;
import java.util.UUID;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface InvoiceItemDaoDomainMapper {
  InvoiceItem toDomain(InvoiceItemEntity dao);

  @Mapping(target = "invoiceDraftId", source = "invoiceDraftId")
  @Mapping(target = "position", source = "position")
  InvoiceItemEntity toDao(InvoiceItem item, UUID invoiceDraftId, int position);
}
