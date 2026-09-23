package com.invoiceautomationservice.domain.model;

public enum IdentityDocumentType {
  DNI(8, "1"),
  RUC(11, "6");

  private final int length;
  private final String nubefactCode;

  IdentityDocumentType(int length, String nubefactCode) {
    this.length = length;
    this.nubefactCode = nubefactCode;
  }

  public int length() {
    return length;
  }

  public String nubefactCode() {
    return nubefactCode;
  }
}
