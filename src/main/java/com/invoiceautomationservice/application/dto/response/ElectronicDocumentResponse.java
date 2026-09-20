package com.invoiceautomationservice.application.dto.response;

import com.invoiceautomationservice.domain.model.IdentityDocumentType;
import com.invoiceautomationservice.domain.model.InvoiceDocumentType;
import com.invoiceautomationservice.domain.model.ElectronicDocumentStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ElectronicDocumentResponse(
    UUID id,
    UUID draftId,
    String companyId,
    String customerId,
    InvoiceDocumentType documentType,
    String series,
    long correlative,
    String fullNumber,
    IdentityDocumentType recipientDocumentType,
    String recipientDocumentNumber,
    String currency,
    List<InvoiceItemResponse> items,
    BigDecimal subtotal,
    BigDecimal discountTotal,
    BigDecimal taxableTotal,
    BigDecimal taxTotal,
    BigDecimal total,
    ElectronicDocumentStatus status,
    String providerReference,
    Instant submittedAt,
    Instant respondedAt,
    String providerResponseCode,
    String providerResponseMessage
) {
}
