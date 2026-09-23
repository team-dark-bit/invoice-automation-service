package com.invoiceautomationservice.application.service;

import com.invoiceautomationservice.domain.model.IdentityDocumentType;
import com.invoiceautomationservice.domain.model.InvoiceDocumentType;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class ConversationCommandParser {
  private static final Pattern START = Pattern.compile(
      "^NUEVA\\s+(BOLETA|FACTURA)\\s+(DNI|RUC)\\s+(\\d{8}|\\d{11})(?:\\s+(PEN|USD))?$",
      Pattern.CASE_INSENSITIVE);

  public Command parse(String text) {
    String normalized = normalize(text);
    var start = START.matcher(normalized);
    if (start.matches()) {
      InvoiceDocumentType documentType = start.group(1).equalsIgnoreCase("FACTURA")
          ? InvoiceDocumentType.INVOICE : InvoiceDocumentType.SALES_RECEIPT;
      IdentityDocumentType identityType = IdentityDocumentType.valueOf(
          start.group(2).toUpperCase(Locale.ROOT));
      return new StartCommand(documentType, identityType, start.group(3),
          start.group(4) == null ? "PEN" : start.group(4).toUpperCase(Locale.ROOT));
    }
    if (normalized.toUpperCase(Locale.ROOT).startsWith("AGREGAR ")) {
      String[] parts = normalized.substring(8).split("\\|", -1);
      if (parts.length == 3) {
        try {
          return new AddItemCommand(parts[1].strip(), new BigDecimal(parts[0].strip()),
              new BigDecimal(parts[2].strip()));
        } catch (NumberFormatException ignored) {
          return new UnknownCommand();
        }
      }
    }
    return switch (normalized.toUpperCase(Locale.ROOT)) {
      case "AYUDA", "HELP", "MENU" -> new HelpCommand();
      case "RESUMEN" -> new SummaryCommand();
      case "GENERAR" -> new GenerateCommand();
      case "CANCELAR" -> new ResetCommand();
      default -> new UnknownCommand();
    };
  }

  private String normalize(String value) {
    return Normalizer.normalize(value.strip(), Normalizer.Form.NFKC)
        .replaceAll("\\s+", " ");
  }

  public sealed interface Command permits StartCommand, AddItemCommand, HelpCommand,
      SummaryCommand, GenerateCommand, ResetCommand, UnknownCommand {}
  public record StartCommand(InvoiceDocumentType documentType,
      IdentityDocumentType identityType, String documentNumber, String currency) implements Command {}
  public record AddItemCommand(String description, BigDecimal quantity,
      BigDecimal unitPrice) implements Command {}
  public record HelpCommand() implements Command {}
  public record SummaryCommand() implements Command {}
  public record GenerateCommand() implements Command {}
  public record ResetCommand() implements Command {}
  public record UnknownCommand() implements Command {}
}
