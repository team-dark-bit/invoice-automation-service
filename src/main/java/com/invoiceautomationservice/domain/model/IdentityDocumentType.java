package com.invoiceautomationservice.domain.model;

public enum IdentityDocumentType {
  DNI(8),
  RUC(11);

  private final int length;

  IdentityDocumentType(int length) {
    this.length = length;
  }

  public int length() {
    return length;
  }
}
