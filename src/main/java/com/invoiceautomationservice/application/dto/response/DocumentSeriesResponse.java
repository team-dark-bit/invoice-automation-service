package com.invoiceautomationservice.application.dto.response;

import com.invoiceautomationservice.domain.model.InvoiceDocumentType;

public record DocumentSeriesResponse(
    String id,
    String companyId,
    InvoiceDocumentType documentType,
    String series,
    long currentCorrelative,
    boolean active
) {
}
