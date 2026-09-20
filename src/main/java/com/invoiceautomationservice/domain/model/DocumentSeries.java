package com.invoiceautomationservice.domain.model;

public record DocumentSeries(
    String id,
    String companyId,
    InvoiceDocumentType documentType,
    String series,
    long currentCorrelative,
    boolean active
) {
}
