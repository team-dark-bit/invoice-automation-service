package com.invoiceautomationservice.application.service.mapper;

import com.invoiceautomationservice.application.dto.response.InvoiceDraftResponse;
import com.invoiceautomationservice.application.dto.response.InvoiceItemResponse;
import com.invoiceautomationservice.domain.model.InvoiceDraft;
import com.invoiceautomationservice.domain.model.InvoiceItem;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface InvoiceDraftDomainResponseMapper {
  InvoiceDraftResponse toResponse(InvoiceDraft invoiceDraft);
  InvoiceItemResponse toResponse(InvoiceItem invoiceItem);
}
