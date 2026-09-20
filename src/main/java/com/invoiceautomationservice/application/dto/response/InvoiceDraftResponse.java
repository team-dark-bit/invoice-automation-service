package com.invoiceautomationservice.application.dto.response;

import com.invoiceautomationservice.domain.model.InvoiceDraftStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import com.invoiceautomationservice.domain.model.IdentityDocumentType;
import com.invoiceautomationservice.domain.model.InvoiceDocumentType;

public record InvoiceDraftResponse(
        UUID id,
        String companyId,
        String customerId,
        InvoiceDocumentType documentType,
        IdentityDocumentType recipientDocumentType,
        String recipientDocumentNumber,
        String currency,
        InvoiceDraftStatus status,
        List<InvoiceItemResponse> items,
        BigDecimal subtotal,
        BigDecimal discountTotal,
        BigDecimal taxableTotal,
        BigDecimal taxTotal,
        BigDecimal total,
        Instant createdAt,
        Instant updatedAt,
        String providerReference,
        Instant issuedAt
) {
  public InvoiceDraftResponse(
      UUID id, String companyId, String customerId, InvoiceDocumentType documentType,
      IdentityDocumentType recipientDocumentType, String recipientDocumentNumber,
      String currency, InvoiceDraftStatus status, List<InvoiceItemResponse> items,
      BigDecimal subtotal, BigDecimal total, Instant createdAt, Instant updatedAt,
      String providerReference, Instant issuedAt) {
    this(id, companyId, customerId, documentType, recipientDocumentType,
        recipientDocumentNumber, currency, status, items, subtotal, BigDecimal.ZERO,
        BigDecimal.ZERO, BigDecimal.ZERO, total, createdAt, updatedAt, providerReference, issuedAt);
  }
}
