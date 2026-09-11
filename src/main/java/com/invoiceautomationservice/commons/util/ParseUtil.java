package com.invoiceautomationservice.commons.util;

public class ParseUtil {
  public static Double mapStringToDouble(String value) {
    if (value == null) return null;
    value = value.trim();
    if (value.isEmpty()) return null;
    try {
      return Double.valueOf(value);
    } catch (NumberFormatException e) {
      return null;
    }
  }
}
