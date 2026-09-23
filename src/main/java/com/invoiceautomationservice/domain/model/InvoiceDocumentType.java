package com.invoiceautomationservice.domain.model;

public enum InvoiceDocumentType {
  INVOICE("01", "1"),
  SALES_RECEIPT("03", "2");

  private final String sunatCode;
  private final String nubefactCode;

  InvoiceDocumentType(String sunatCode, String nubefactCode) {
    this.sunatCode = sunatCode;
    this.nubefactCode = nubefactCode;
  }

  public String sunatCode() {
    return sunatCode;
  }

  public String nubefactCode() {
    return nubefactCode;
  }
}
