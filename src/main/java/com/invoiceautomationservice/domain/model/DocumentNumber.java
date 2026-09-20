package com.invoiceautomationservice.domain.model;

public record DocumentNumber(String series, long correlative) {
  public String fullNumber() {
    return "%s-%08d".formatted(series, correlative);
  }
}
