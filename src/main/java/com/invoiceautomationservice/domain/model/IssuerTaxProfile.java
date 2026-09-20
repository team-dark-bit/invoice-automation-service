package com.invoiceautomationservice.domain.model;

import java.time.Instant;
import java.util.Objects;

public record IssuerTaxProfile(
    String companyId,
    TaxpayerType taxpayerType,
    String fiscalAddress,
    String ubigeo,
    String department,
    String province,
    String district,
    String countryCode,
    Instant createdAt,
    Instant updatedAt
) {
  public IssuerTaxProfile {
    Objects.requireNonNull(companyId, "companyId is required");
    Objects.requireNonNull(taxpayerType, "taxpayerType is required");
    Objects.requireNonNull(fiscalAddress, "fiscalAddress is required");
    Objects.requireNonNull(ubigeo, "ubigeo is required");
    Objects.requireNonNull(countryCode, "countryCode is required");
    Objects.requireNonNull(createdAt, "createdAt is required");
    Objects.requireNonNull(updatedAt, "updatedAt is required");
    if (!ubigeo.matches("\\d{6}")) {
      throw new IllegalArgumentException("ubigeo must contain 6 digits");
    }
    if (!countryCode.matches("[A-Z]{2}")) {
      throw new IllegalArgumentException("countryCode must be an ISO alpha-2 code");
    }
  }
}
