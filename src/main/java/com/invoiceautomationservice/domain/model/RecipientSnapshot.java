package com.invoiceautomationservice.domain.model;

import java.util.Objects;

public record RecipientSnapshot(
    IdentityDocumentType documentType,
    String documentNumber,
    String name,
    String address,
    String email
) {
  public RecipientSnapshot {
    Objects.requireNonNull(documentType, "recipient documentType is required");
    Objects.requireNonNull(documentNumber, "recipient documentNumber is required");
    Objects.requireNonNull(name, "recipient name is required");
    if (name.isBlank()) {
      throw new IllegalArgumentException("recipient name must not be blank");
    }
  }
}
