package com.invoiceautomationservice.application.dto.response;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record ConversationReviewResponse(
    Map<String, String> detectedValues,
    Map<String, String> confirmedValues,
    List<String> missingFields,
    List<String> ambiguousFields,
    BigDecimal confidence,
    List<String> calculationErrors,
    boolean confirmationRequired
) {
  public ConversationReviewResponse {
    detectedValues = Map.copyOf(detectedValues);
    confirmedValues = Map.copyOf(confirmedValues);
    missingFields = List.copyOf(missingFields);
    ambiguousFields = List.copyOf(ambiguousFields);
    calculationErrors = List.copyOf(calculationErrors);
  }
}
