package com.invoiceautomationservice.domain.model;

public enum InvoiceDocumentType {
  INVOICE("01"),
  SALES_RECEIPT("03");

  private final String sunatCode;

  InvoiceDocumentType(String sunatCode) {
    this.sunatCode = sunatCode;
  }

  public String sunatCode() {
    return sunatCode;
  }
}
