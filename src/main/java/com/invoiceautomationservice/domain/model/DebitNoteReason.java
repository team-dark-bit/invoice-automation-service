package com.invoiceautomationservice.domain.model;

import java.util.Arrays;

public enum DebitNoteReason {
  INTEREST("01"),
  VALUE_INCREASE("02"),
  PENALTIES("03");

  private final String code;
  DebitNoteReason(String code) { this.code = code; }
  public String code() { return code; }
  public static DebitNoteReason fromCode(String code) {
    return Arrays.stream(values()).filter(value -> value.code.equals(code)).findFirst()
        .orElseThrow(() -> new IllegalArgumentException("invalid debit note reason: " + code));
  }
}
