package com.invoiceautomationservice.domain.model;

import java.math.BigDecimal;

public enum TaxAffectation {
  TAXED("10", "1", new BigDecimal("18.00")),
  EXEMPT("20", "8", BigDecimal.ZERO),
  UNAFFECTED("30", "9", BigDecimal.ZERO);

  private final String sunatCode;
  private final String nubefactCode;
  private final BigDecimal taxRate;

  TaxAffectation(String sunatCode, String nubefactCode, BigDecimal taxRate) {
    this.sunatCode = sunatCode;
    this.nubefactCode = nubefactCode;
    this.taxRate = taxRate;
  }

  public String nubefactCode() {
    return nubefactCode;
  }

  public String sunatCode() {
    return sunatCode;
  }

  public BigDecimal taxRate() {
    return taxRate;
  }
}
