package com.invoiceautomationservice.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record ElectronicDocument(
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
    List<InvoiceItem> items,
    BigDecimal subtotal,
    BigDecimal discountTotal,
    BigDecimal taxableTotal,
    BigDecimal taxTotal,
    BigDecimal total,
    String providerReference,
    Instant issuedAt
) {
  public ElectronicDocument {
    Objects.requireNonNull(id);
    Objects.requireNonNull(draftId);
    Objects.requireNonNull(companyId);
    Objects.requireNonNull(customerId);
    Objects.requireNonNull(documentType);
    Objects.requireNonNull(series);
    Objects.requireNonNull(fullNumber);
    Objects.requireNonNull(recipientDocumentType);
    Objects.requireNonNull(recipientDocumentNumber);
    Objects.requireNonNull(currency);
    items = List.copyOf(items);
    if ((providerReference == null) != (issuedAt == null)) {
      throw new IllegalArgumentException("provider reference and issuedAt must be provided together");
    }
  }

  public static ElectronicDocument from(InvoiceDraft draft, DocumentNumber number) {
    return new ElectronicDocument(
        UUID.randomUUID(), draft.id(), draft.companyId(), draft.customerId(), draft.documentType(),
        number.series(), number.correlative(), number.fullNumber(), draft.recipientDocumentType(),
        draft.recipientDocumentNumber(), draft.currency(), draft.items(), draft.subtotal(),
        draft.discountTotal(), draft.taxableTotal(), draft.taxTotal(), draft.total(), null, null);
  }

  public ElectronicDocument issued(BillingResult result) {
    return new ElectronicDocument(
        id, draftId, companyId, customerId, documentType, series, correlative, fullNumber,
        recipientDocumentType, recipientDocumentNumber, currency, items, subtotal, discountTotal,
        taxableTotal, taxTotal, total, result.reference(), result.issuedAt());
  }
}
