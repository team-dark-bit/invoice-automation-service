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
    ElectronicDocumentStatus status,
    String providerReference,
    Instant submittedAt,
    Instant respondedAt,
    String providerResponseCode,
    String providerResponseMessage
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
    Objects.requireNonNull(status);
    items = List.copyOf(items);
    if (status == ElectronicDocumentStatus.PENDING_SEND
        && (providerReference != null || submittedAt != null)) {
      throw new IllegalArgumentException("a pending document cannot contain provider submission data");
    }
  }

  public static ElectronicDocument from(InvoiceDraft draft, DocumentNumber number) {
    return new ElectronicDocument(
        UUID.randomUUID(), draft.id(), draft.companyId(), draft.customerId(), draft.documentType(),
        number.series(), number.correlative(), number.fullNumber(), draft.recipientDocumentType(),
        draft.recipientDocumentNumber(), draft.currency(), draft.items(), draft.subtotal(),
        draft.discountTotal(), draft.taxableTotal(), draft.taxTotal(), draft.total(),
        ElectronicDocumentStatus.PENDING_SEND, null, null, null, null, null);
  }

  public ElectronicDocument withBillingResult(BillingResult result) {
    if (status == ElectronicDocumentStatus.ACCEPTED
        || status == ElectronicDocumentStatus.REJECTED) {
      throw new IllegalStateException("a document with final provider status cannot be changed");
    }
    return new ElectronicDocument(
        id, draftId, companyId, customerId, documentType, series, correlative, fullNumber,
        recipientDocumentType, recipientDocumentNumber, currency, items, subtotal, discountTotal,
        taxableTotal, taxTotal, total, result.status(), result.reference(),
        submittedAt == null ? result.submittedAt() : submittedAt,
        result.respondedAt(), result.responseCode(), result.responseMessage());
  }
}
