package com.invoiceautomationservice.domain.model;

public record RecipientLookupResult(
    String fullName,
    String companyName,
    String address,
    String email
) {
  public RecipientLookupResult(String fullName, String companyName) {
    this(fullName, companyName, null, null);
  }
}
