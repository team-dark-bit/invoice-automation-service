package com.invoiceautomationservice.domain.model;

import java.util.Arrays;

public enum CreditNoteReason {
  OPERATION_CANCELLATION("01"),
  RUC_ERROR_CANCELLATION("02"),
  DESCRIPTION_CORRECTION("03"),
  GLOBAL_DISCOUNT("04"),
  ITEM_DISCOUNT("05"),
  TOTAL_RETURN("06"),
  ITEM_RETURN("07"),
  BONUS("08"),
  VALUE_DECREASE("09"),
  OTHER("10");

  private final String code;
  CreditNoteReason(String code) { this.code = code; }
  public String code() { return code; }
  public static CreditNoteReason fromCode(String code) {
    return Arrays.stream(values()).filter(value -> value.code.equals(code)).findFirst()
        .orElseThrow(() -> new IllegalArgumentException("invalid credit note reason: " + code));
  }
}
