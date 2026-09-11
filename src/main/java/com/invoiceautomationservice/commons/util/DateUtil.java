package com.invoiceautomationservice.commons.util;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

public class DateUtil {

  public static LocalDateTime toLocalDateTime(String date) {
    OffsetDateTime offsetDateTime = OffsetDateTime.parse(date);
    return offsetDateTime.toLocalDateTime();
  }
}
