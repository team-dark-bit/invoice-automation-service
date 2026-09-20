package com.invoiceautomationservice.domain.model;

import com.invoiceautomationservice.domain.exception.InvalidInvoiceDraftStateException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record InvoiceDraft(
        UUID id,
        String companyId,
        String customerId,
        InvoiceDocumentType documentType,
        IdentityDocumentType recipientDocumentType,
        String recipientDocumentNumber,
        String currency,
        InvoiceDraftStatus status,
        List<InvoiceItem> items,
        BigDecimal subtotal,
        BigDecimal total,
        Instant createdAt,
        Instant updatedAt,
        String providerReference,
        Instant issuedAt
) {

  public InvoiceDraft {
    Objects.requireNonNull(id, "id is required");
    Objects.requireNonNull(companyId, "companyId is required");
    Objects.requireNonNull(customerId, "customerId is required");
    Objects.requireNonNull(documentType, "documentType is required");
    Objects.requireNonNull(recipientDocumentType, "recipientDocumentType is required");
    Objects.requireNonNull(recipientDocumentNumber, "recipientDocumentNumber is required");
    Objects.requireNonNull(currency, "currency is required");
    Objects.requireNonNull(status, "status is required");
    Objects.requireNonNull(items, "items are required");
    Objects.requireNonNull(subtotal, "subtotal is required");
    Objects.requireNonNull(total, "total is required");
    Objects.requireNonNull(createdAt, "createdAt is required");
    Objects.requireNonNull(updatedAt, "updatedAt is required");
    items = List.copyOf(items);
    if (!currency.matches("[A-Z]{3}")) {
      throw new IllegalArgumentException("currency must be a three-letter ISO code");
    }
    if (items.isEmpty()) {
      throw new IllegalArgumentException("invoice draft must contain at least one item");
    }
    if (documentType == InvoiceDocumentType.INVOICE
        && recipientDocumentType != IdentityDocumentType.RUC) {
      throw new IllegalArgumentException("invoice recipient must be identified with RUC");
    }
    if (status == InvoiceDraftStatus.ISSUED && (providerReference == null || issuedAt == null)) {
      throw new IllegalArgumentException("issued draft requires provider reference and issue date");
    }
  }

  public static InvoiceDraft create(
          String companyId,
          String customerId,
          InvoiceDocumentType documentType,
          IdentityDocumentType recipientDocumentType,
          String recipientDocumentNumber,
          String currency,
          List<InvoiceItem> items,
          Instant createdAt
  ) {
    BigDecimal subtotal = items.stream()
            .map(InvoiceItem::lineTotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    return new InvoiceDraft(
            UUID.randomUUID(), companyId, customerId, documentType, recipientDocumentType,
            recipientDocumentNumber, currency, InvoiceDraftStatus.DRAFT,
            items, subtotal, subtotal, createdAt, createdAt, null, null
    );
  }

  public InvoiceDraft approve(Instant approvedAt) {
    if (status != InvoiceDraftStatus.DRAFT) {
      throw new InvalidInvoiceDraftStateException(status, InvoiceDraftStatus.DRAFT);
    }
    return copyWith(InvoiceDraftStatus.APPROVED, approvedAt, null, null);
  }

  public InvoiceDraft markIssued(String reference, Instant issueDate) {
    ensureCanBeIssued();
    if (reference == null || reference.isBlank()) {
      throw new IllegalArgumentException("provider reference is required");
    }
    return copyWith(InvoiceDraftStatus.ISSUED, issueDate, reference, issueDate);
  }

  public void ensureCanBeIssued() {
    if (status != InvoiceDraftStatus.APPROVED) {
      throw new InvalidInvoiceDraftStateException(status, InvoiceDraftStatus.APPROVED);
    }
  }

  private InvoiceDraft copyWith(
          InvoiceDraftStatus newStatus,
          Instant newUpdatedAt,
          String newProviderReference,
          Instant newIssuedAt
  ) {
    return new InvoiceDraft(
            id, companyId, customerId, documentType, recipientDocumentType,
            recipientDocumentNumber, currency, newStatus, items, subtotal, total,
            createdAt, newUpdatedAt, newProviderReference, newIssuedAt
    );
  }

}
