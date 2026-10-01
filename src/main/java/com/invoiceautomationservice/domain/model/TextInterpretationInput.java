package com.invoiceautomationservice.domain.model;

import java.util.Objects;
import java.util.UUID;

public record TextInterpretationInput(
    String companyId,
    UUID conversationId,
    String text,
    InterpretationContextSnapshot context
) {
  public TextInterpretationInput {
    companyId = requireText(companyId, "companyId");
    Objects.requireNonNull(conversationId, "conversationId must not be null");
    text = requireText(text, "text");
    Objects.requireNonNull(context, "context must not be null");
  }

  private static String requireText(String value, String field) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(field + " must not be blank");
    }
    return value.strip();
  }
}
