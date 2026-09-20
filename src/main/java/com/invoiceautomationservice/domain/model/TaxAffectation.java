package com.invoiceautomationservice.domain.model;

import java.math.BigDecimal;

public enum TaxAffectation {
  TAXED("10", new BigDecimal("18.00")),
  EXEMPT("20", BigDecimal.ZERO),
  UNAFFECTED("30", BigDecimal.ZERO);

  private final String sunatCode;
  private final BigDecimal taxRate;

  TaxAffectation(String sunatCode, BigDecimal taxRate) {
    this.sunatCode = sunatCode;
    this.taxRate = taxRate;
  }

  public String sunatCode() {
    return sunatCode;
  }

  public BigDecimal taxRate() {
    return taxRate;
  }
}
