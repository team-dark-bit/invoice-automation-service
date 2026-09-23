package com.invoiceautomationservice.domain.model;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.Objects;
import java.time.Instant;

public record BillingSubmission(
    UUID documentId,
    String idempotencyKey,
    String fullNumber,
    String series,
    long correlative,
    InvoiceDocumentType documentType,
    String operation,
    String sunatTransaction,
    Instant emissionAt,
    String currency,
    String currencyCode,
    Issuer issuer,
    Recipient recipient,
    List<Item> items,
    BigDecimal subtotal,
    BigDecimal discountTotal,
    BigDecimal taxableTotal,
    BigDecimal exemptTotal,
    BigDecimal unaffectedTotal,
    BigDecimal taxTotal,
    BigDecimal total,
    BigDecimal igvPercentage,
    boolean sendAutomaticallyToSunat,
    boolean sendAutomaticallyToCustomer
) {
  public BillingSubmission {
    Objects.requireNonNull(documentId, "documentId is required");
    Objects.requireNonNull(idempotencyKey, "idempotencyKey is required");
    Objects.requireNonNull(emissionAt, "emissionAt is required");
    if (idempotencyKey.isBlank()) {
      throw new IllegalArgumentException("idempotencyKey must not be blank");
    }
    items = List.copyOf(items);
  }

  public record Issuer(
      String taxId,
      String legalName,
      String tradeName,
      TaxpayerType taxpayerType,
      String fiscalAddress,
      String ubigeo,
      String department,
      String province,
      String district,
      String countryCode
  ) {}

  public record Recipient(
      IdentityDocumentType documentType,
      String documentNumber,
      String name,
      String address,
      String email
  ) {}

  public record Item(
      String code,
      String description,
      UnitCode unitCode,
      BigDecimal quantity,
      BigDecimal unitPrice,
      BigDecimal discount,
      TaxAffectation taxAffectation,
      BigDecimal taxRate,
      BigDecimal grossAmount,
      BigDecimal taxableAmount,
      BigDecimal taxAmount,
      BigDecimal lineTotal,
      BigDecimal unitValue,
      BigDecimal unitPriceWithTax,
      BigDecimal subtotal,
      String igvTypeCode
  ) {}
}
