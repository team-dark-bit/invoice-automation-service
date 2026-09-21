package com.invoiceautomationservice.domain.model;

import java.util.Objects;

public record IssuerSnapshot(
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
) {
  public IssuerSnapshot {
    Objects.requireNonNull(taxId, "issuer taxId is required");
    Objects.requireNonNull(legalName, "issuer legalName is required");
    Objects.requireNonNull(taxpayerType, "issuer taxpayerType is required");
    Objects.requireNonNull(fiscalAddress, "issuer fiscalAddress is required");
    Objects.requireNonNull(ubigeo, "issuer ubigeo is required");
    Objects.requireNonNull(countryCode, "issuer countryCode is required");
  }
}
