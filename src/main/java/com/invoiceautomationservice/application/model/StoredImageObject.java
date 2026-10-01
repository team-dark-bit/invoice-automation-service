package com.invoiceautomationservice.application.model;

import java.util.Objects;

public record StoredImageObject(String key) {
  public StoredImageObject {
    Objects.requireNonNull(key, "storage key is required");
    if (key.isBlank()) {
      throw new IllegalArgumentException("storage key must not be blank");
    }
  }
}
