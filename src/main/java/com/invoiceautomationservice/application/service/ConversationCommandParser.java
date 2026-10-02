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
  private static final Pattern CHANGE_QUANTITY = Pattern.compile(
      "^(?:CAMBIA|CAMBIAR|CORRIGE|ACTUALIZA)\\s+LA\\s+CANTIDAD\\s+DE\\s+(.+?)\\s+A\\s+([0-9]+(?:[.,][0-9]+)?)$",
      Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
  private static final Pattern CHANGE_PRICE = Pattern.compile(
      "^(?:CAMBIA|CAMBIAR|CORRIGE|ACTUALIZA)\\s+EL\\s+PRECIO(?:\\s+UNITARIO)?\\s+DE\\s+(.+?)\\s+A\\s+([0-9]+(?:[.,][0-9]+)?)$",
      Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
  private static final Pattern PRICE_IS = Pattern.compile(
      "^EL\\s+PRECIO(?:\\s+UNITARIO)?\\s+ES\\s+([0-9]+(?:[.,][0-9]+)?)(?:\\s+(?:PARA|DE)\\s+(.+))?$",
      Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
  private static final Pattern REMOVE_ITEM = Pattern.compile(
      "^(?:ELIMINA|ELIMINAR|QUITA|QUITAR)\\s+(?:EL|LA|LOS|LAS)?\\s*(.+)$",
      Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
  private static final Pattern CORRECT_RECIPIENT = Pattern.compile(
      "^(?:EL\\s+)?(DNI|RUC)\\s+CORRECTO\\s+ES\\s+(\\d+)$",
      Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
  private static final Pattern CORRECT_DOCUMENT_TYPE = Pattern.compile(
      "^ES\\s+(FACTURA|BOLETA)(?:,?\\s+NO\\s+(?:FACTURA|BOLETA))?$",
      Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

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
    var quantity = CHANGE_QUANTITY.matcher(normalized);
    if (quantity.matches()) {
      return new ChangeItemQuantityCommand(quantity.group(1).strip(), decimal(quantity.group(2)));
    }
    var price = CHANGE_PRICE.matcher(normalized);
    if (price.matches()) {
      return new ChangeItemPriceCommand(price.group(1).strip(), decimal(price.group(2)));
    }
    var priceIs = PRICE_IS.matcher(normalized);
    if (priceIs.matches()) {
      return new ChangeItemPriceCommand(
          priceIs.group(2) == null ? null : priceIs.group(2).strip(), decimal(priceIs.group(1)));
    }
    var remove = REMOVE_ITEM.matcher(normalized);
    if (remove.matches()) {
      return new RemoveItemCommand(remove.group(1).strip());
    }
    var recipient = CORRECT_RECIPIENT.matcher(normalized);
    if (recipient.matches()) {
      return new CorrectRecipientCommand(IdentityDocumentType.valueOf(
          recipient.group(1).toUpperCase(Locale.ROOT)), recipient.group(2));
    }
    var documentType = CORRECT_DOCUMENT_TYPE.matcher(normalized);
    if (documentType.matches()) {
      return new CorrectDocumentTypeCommand(documentType.group(1).equalsIgnoreCase("FACTURA")
          ? InvoiceDocumentType.INVOICE : InvoiceDocumentType.SALES_RECEIPT);
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
      case "CONFIRMAR", "SI", "SÍ" -> new ConfirmCommand();
      case "GENERAR" -> new GenerateCommand();
      case "CANCELAR" -> new ResetCommand();
      default -> new UnknownCommand();
    };
  }

  private String normalize(String value) {
    return Normalizer.normalize(value.strip(), Normalizer.Form.NFKC)
        .replaceAll("\\s+", " ");
  }

  private BigDecimal decimal(String value) {
    return new BigDecimal(value.replace(',', '.'));
  }

  public sealed interface Command permits StartCommand, AddItemCommand, HelpCommand,
      SummaryCommand, ConfirmCommand, GenerateCommand, ResetCommand, UnknownCommand,
      ChangeItemQuantityCommand, ChangeItemPriceCommand, RemoveItemCommand,
      CorrectRecipientCommand, CorrectDocumentTypeCommand {}
  public record StartCommand(InvoiceDocumentType documentType,
      IdentityDocumentType identityType, String documentNumber, String currency) implements Command {}
  public record AddItemCommand(String description, BigDecimal quantity,
      BigDecimal unitPrice) implements Command {}
  public record ChangeItemQuantityCommand(String itemReference, BigDecimal quantity)
      implements Command {}
  public record ChangeItemPriceCommand(String itemReference, BigDecimal unitPrice)
      implements Command {}
  public record RemoveItemCommand(String itemReference) implements Command {}
  public record CorrectRecipientCommand(IdentityDocumentType identityType, String documentNumber)
      implements Command {}
  public record CorrectDocumentTypeCommand(InvoiceDocumentType documentType) implements Command {}
  public record HelpCommand() implements Command {}
  public record SummaryCommand() implements Command {}
  public record ConfirmCommand() implements Command {}
  public record GenerateCommand() implements Command {}
  public record ResetCommand() implements Command {}
  public record UnknownCommand() implements Command {}
}
