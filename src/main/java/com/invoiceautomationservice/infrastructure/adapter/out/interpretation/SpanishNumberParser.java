package com.invoiceautomationservice.infrastructure.adapter.out.interpretation;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

final class SpanishNumberParser {
  private static final Map<String, Integer> VALUES = Map.ofEntries(
      Map.entry("cero", 0), Map.entry("un", 1), Map.entry("uno", 1),
      Map.entry("una", 1), Map.entry("dos", 2), Map.entry("tres", 3),
      Map.entry("cuatro", 4), Map.entry("cinco", 5), Map.entry("seis", 6),
      Map.entry("siete", 7), Map.entry("ocho", 8), Map.entry("nueve", 9),
      Map.entry("diez", 10), Map.entry("once", 11), Map.entry("doce", 12),
      Map.entry("trece", 13), Map.entry("catorce", 14), Map.entry("quince", 15),
      Map.entry("dieciseis", 16), Map.entry("diecisiete", 17),
      Map.entry("dieciocho", 18), Map.entry("diecinueve", 19),
      Map.entry("veinte", 20), Map.entry("veintiun", 21), Map.entry("veintiuno", 21),
      Map.entry("veintiuna", 21), Map.entry("veintidos", 22), Map.entry("veintitres", 23),
      Map.entry("veinticuatro", 24), Map.entry("veinticinco", 25),
      Map.entry("veintiseis", 26), Map.entry("veintisiete", 27),
      Map.entry("veintiocho", 28), Map.entry("veintinueve", 29),
      Map.entry("treinta", 30), Map.entry("cuarenta", 40), Map.entry("cincuenta", 50),
      Map.entry("sesenta", 60), Map.entry("setenta", 70), Map.entry("ochenta", 80),
      Map.entry("noventa", 90), Map.entry("cien", 100), Map.entry("ciento", 100),
      Map.entry("doscientos", 200), Map.entry("trescientos", 300),
      Map.entry("cuatrocientos", 400), Map.entry("quinientos", 500),
      Map.entry("seiscientos", 600), Map.entry("setecientos", 700),
      Map.entry("ochocientos", 800), Map.entry("novecientos", 900));

  private SpanishNumberParser() {}

  static Optional<BigDecimal> parse(String value) {
    if (value == null || value.isBlank()) {
      return Optional.empty();
    }
    String normalized = normalize(value)
        .replaceAll("\\b(?:sol(?:es)?|pen|dolar(?:es)?|usd)\\b", "")
        .strip();
    try {
      return Optional.of(new BigDecimal(normalized.replace(',', '.')));
    } catch (NumberFormatException ignored) {
      // Continue with Spanish number words.
    }
    String[] decimalParts = normalized.split("\\s+con\\s+", 2);
    Integer integer = parseIntegerWords(decimalParts[0]);
    if (integer == null) {
      return Optional.empty();
    }
    if (decimalParts.length == 1) {
      return Optional.of(BigDecimal.valueOf(integer));
    }
    Integer fraction = parseIntegerWords(decimalParts[1]);
    if (fraction == null || fraction < 0 || fraction > 99) {
      return Optional.empty();
    }
    return Optional.of(BigDecimal.valueOf(integer)
        .add(BigDecimal.valueOf(fraction, 2)));
  }

  static PrefixNumber parsePrefix(String value) {
    String normalized = normalize(value);
    String[] tokens = normalized.split("\\s+");
    for (int length = Math.min(5, tokens.length); length >= 1; length--) {
      String candidate = String.join(" ", java.util.Arrays.copyOfRange(tokens, 0, length));
      Optional<BigDecimal> number = parse(candidate);
      if (number.isPresent()) {
        String remainder = String.join(" ",
            java.util.Arrays.copyOfRange(tokens, length, tokens.length)).strip();
        return new PrefixNumber(number.get(), remainder);
      }
    }
    return null;
  }

  private static Integer parseIntegerWords(String value) {
    String[] tokens = normalize(value).split("\\s+");
    int total = 0;
    boolean found = false;
    for (String token : tokens) {
      if (token.equals("y")) {
        continue;
      }
      Integer number = VALUES.get(token);
      if (number == null) {
        return null;
      }
      total += number;
      found = true;
    }
    return found ? total : null;
  }

  private static String normalize(String value) {
    String normalized = Normalizer.normalize(value.strip().toLowerCase(Locale.ROOT),
        Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    return normalized.replaceAll("\\s+", " ");
  }

  record PrefixNumber(BigDecimal value, String remainder) {}
}
