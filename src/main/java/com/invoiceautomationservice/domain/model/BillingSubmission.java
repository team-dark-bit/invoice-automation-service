package com.invoiceautomationservice.domain.model;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record BillingSubmission(
    UUID documentId,
    String fullNumber,
    String series,
    long correlative,
    InvoiceDocumentType documentType,
    String currency,
    Issuer issuer,
    Recipient recipient,
    List<Item> items,
    BigDecimal subtotal,
    BigDecimal discountTotal,
    BigDecimal taxableTotal,
    BigDecimal taxTotal,
    BigDecimal total
) {
  public BillingSubmission {
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
      String name
  ) {}

  public record Item(
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
      BigDecimal lineTotal
  ) {}
}
