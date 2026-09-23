package com.invoiceautomationservice.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.nio.charset.StandardCharsets;

public record ElectronicDocument(
    UUID id,
    UUID draftId,
    String companyId,
    String customerId,
    IssuerSnapshot issuer,
    RecipientSnapshot recipient,
    InvoiceDocumentType documentType,
    UUID relatedDocumentId,
    InvoiceDocumentType relatedDocumentType,
    String relatedSeries,
    Long relatedCorrelative,
    String noteReasonCode,
    String noteReason,
    String series,
    long correlative,
    String fullNumber,
    IdentityDocumentType recipientDocumentType,
    String recipientDocumentNumber,
    String currency,
    Instant emissionAt,
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
    Objects.requireNonNull(companyId);
    Objects.requireNonNull(customerId);
    Objects.requireNonNull(issuer);
    Objects.requireNonNull(recipient);
    Objects.requireNonNull(documentType);
    Objects.requireNonNull(series);
    Objects.requireNonNull(fullNumber);
    Objects.requireNonNull(recipientDocumentType);
    Objects.requireNonNull(recipientDocumentNumber);
    Objects.requireNonNull(currency);
    Objects.requireNonNull(emissionAt);
    Objects.requireNonNull(status);
    if (recipient.documentType() != recipientDocumentType
        || !recipient.documentNumber().equals(recipientDocumentNumber)) {
      throw new IllegalArgumentException("recipient snapshot must match document identity");
    }
    items = List.copyOf(items);
    if (status == ElectronicDocumentStatus.PENDING_SEND
        && (providerReference != null || submittedAt != null)) {
      throw new IllegalArgumentException("a pending document cannot contain provider submission data");
    }
  }

  public static ElectronicDocument from(
      InvoiceDraft draft,
      DocumentNumber number,
      IssuerSnapshot issuer,
      RecipientSnapshot recipient) {
    return new ElectronicDocument(
        deterministicId(draft.id()), draft.id(), draft.companyId(), draft.customerId(), issuer, recipient,
        draft.documentType(), null, null, null, null, null, null,
        number.series(), number.correlative(), number.fullNumber(), draft.recipientDocumentType(),
        draft.recipientDocumentNumber(), draft.currency(), draft.updatedAt(), draft.items(), draft.subtotal(),
        draft.discountTotal(), draft.taxableTotal(), draft.taxTotal(), draft.total(),
        ElectronicDocumentStatus.PENDING_SEND, null, null, null, null, null);
  }

  public static ElectronicDocument noteFrom(
      ElectronicDocument original, DocumentNumber number, InvoiceDocumentType noteType,
      String reasonCode, String reason, Instant emissionAt) {
    if (noteType != InvoiceDocumentType.CREDIT_NOTE
        && noteType != InvoiceDocumentType.DEBIT_NOTE) {
      throw new IllegalArgumentException("note type must be CREDIT_NOTE or DEBIT_NOTE");
    }
    if (original.documentType() != InvoiceDocumentType.INVOICE
        && original.documentType() != InvoiceDocumentType.SALES_RECEIPT) {
      throw new IllegalArgumentException("a note must reference an invoice or sales receipt");
    }
    List<InvoiceItem> noteItems = original.items().stream().map(item -> new InvoiceItem(
        UUID.randomUUID(), item.description(), item.unitCode(), item.quantity(), item.unitPrice(),
        item.discount(), item.taxAffectation(), item.taxRate(), item.grossAmount(),
        item.taxableAmount(), item.taxAmount(), item.lineTotal())).toList();
    UUID id = UUID.nameUUIDFromBytes(("electronic-note:" + original.id() + ":" + noteType
        + ":" + reasonCode).getBytes(StandardCharsets.UTF_8));
    return new ElectronicDocument(
        id, null, original.companyId(), original.customerId(), original.issuer(),
        original.recipient(), noteType, original.id(), original.documentType(), original.series(),
        original.correlative(), reasonCode, reason, number.series(), number.correlative(),
        number.fullNumber(), original.recipientDocumentType(), original.recipientDocumentNumber(),
        original.currency(), emissionAt, noteItems, original.subtotal(), original.discountTotal(),
        original.taxableTotal(), original.taxTotal(), original.total(),
        ElectronicDocumentStatus.PENDING_SEND, null, null, null, null, null);
  }

  private static UUID deterministicId(UUID draftId) {
    return UUID.nameUUIDFromBytes(
        ("electronic-document:" + draftId).getBytes(StandardCharsets.UTF_8));
  }

  public ElectronicDocument withBillingResult(BillingResult result) {
    if (status == ElectronicDocumentStatus.ACCEPTED
        || status == ElectronicDocumentStatus.REJECTED) {
      throw new IllegalStateException("a document with final provider status cannot be changed");
    }
    return new ElectronicDocument(
        id, draftId, companyId, customerId, issuer, recipient, documentType, relatedDocumentId,
        relatedDocumentType, relatedSeries, relatedCorrelative, noteReasonCode, noteReason,
        series, correlative,
        fullNumber,
        recipientDocumentType, recipientDocumentNumber, currency, emissionAt, items, subtotal, discountTotal,
        taxableTotal, taxTotal, total, result.status(), result.reference(),
        submittedAt == null ? result.submittedAt() : submittedAt,
        result.respondedAt(), result.responseCode(), result.responseMessage());
  }

  public ElectronicDocument startSubmission(Instant attemptAt) {
    Objects.requireNonNull(attemptAt, "attemptAt is required");
    if (status != ElectronicDocumentStatus.PENDING_SEND
        && status != ElectronicDocumentStatus.ERROR
        && status != ElectronicDocumentStatus.SENDING) {
      throw new IllegalStateException("document cannot be submitted from status " + status);
    }
    return new ElectronicDocument(
        id, draftId, companyId, customerId, issuer, recipient, documentType, relatedDocumentId,
        relatedDocumentType, relatedSeries, relatedCorrelative, noteReasonCode, noteReason,
        series, correlative,
        fullNumber, recipientDocumentType, recipientDocumentNumber, currency, emissionAt, items, subtotal,
        discountTotal, taxableTotal, taxTotal, total, ElectronicDocumentStatus.SENDING,
        null, attemptAt, null, null, null);
  }
}
