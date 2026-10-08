package com.invoiceautomationservice.infrastructure.adapter.out.interpretation;

import com.invoiceautomationservice.application.port.out.DocumentUnderstandingProvider;
import com.invoiceautomationservice.domain.model.DocumentInterpretation;
import com.invoiceautomationservice.domain.model.IdentityDocumentType;
import com.invoiceautomationservice.domain.model.ImageInterpretationInput;
import com.invoiceautomationservice.domain.model.InterpretationIntent;
import com.invoiceautomationservice.domain.model.InterpretationSource;
import com.invoiceautomationservice.domain.model.InterpretedInvoiceItem;
import com.invoiceautomationservice.domain.model.InvoiceDocumentType;
import com.invoiceautomationservice.domain.model.TaxAffectation;
import com.invoiceautomationservice.domain.model.TextInterpretationInput;
import com.invoiceautomationservice.domain.model.UnitCode;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "ai", name = "provider", havingValue = "rules")
public class RuleBasedDocumentUnderstandingProvider implements DocumentUnderstandingProvider {
  private static final Pattern DNI = Pattern.compile("\\bdni\\s*(?:numero|nro\\.?|#)?\\s*(\\d{8})\\b");
  private static final Pattern RUC = Pattern.compile("\\bruc\\s*(?:numero|nro\\.?|#)?\\s*(\\d{11})\\b");
  private static final Pattern TOTAL = Pattern.compile(
      "(?:[,;]\\s*|\\s+)total\\s*(?:=|:|es)?\\s*(.+)$");
  private static final Pattern PRICE_MARKER = Pattern.compile(
      "\\s+(?:(?:a|por)\\s+(?:un\\s+)?precio(?:\\s+unitario)?(?:\\s+de)?|"
          + "precio(?:\\s+unitario)?(?:\\s*(?:de|es|=|:))?|a|por)\\s+",
      Pattern.CASE_INSENSITIVE);
  private static final Pattern UNIT_SUFFIX = Pattern.compile(
      "\\s*(?:unidades?|unds?\\.?|u\\.)\\s*$", Pattern.CASE_INSENSITIVE);
  private static final String QUANTITY_START =
      "(?:\\d+(?:[.,]\\d+)?|un|uno|una|dos|tres|cuatro|cinco|seis|siete|ocho|nueve|"
          + "diez|once|doce|trece|catorce|quince|dieci\\w+|veinti\\w+|treinta|cuarenta|"
          + "cincuenta|sesenta|setenta|ochenta|noventa|cien|ciento|\\w+cientos)";
  private static final Pattern ITEM_COMMA_SEPARATOR = Pattern.compile(
      ",\\s*(?=" + QUANTITY_START + "\\b)", Pattern.CASE_INSENSITIVE);

  @Override
  public DocumentInterpretation interpretText(TextInterpretationInput input) {
    Objects.requireNonNull(input, "input must not be null");
    String normalized = normalize(input.text());
    if (looksLikeHeader(normalized)) {
      return interpretHeader(input, normalized);
    }
    if (looksLikeItem(normalized)) {
      return interpretItem(input, normalized);
    }
    return DocumentInterpretation.unsupported(InterpretationSource.TEXT,
        "No deterministic natural-language rule matched the message");
  }

  @Override
  public DocumentInterpretation interpretImage(ImageInterpretationInput input) {
    Objects.requireNonNull(input, "input must not be null");
    return DocumentInterpretation.unsupported(InterpretationSource.IMAGE,
        "The rule-based provider cannot interpret images");
  }

  private DocumentInterpretation interpretHeader(TextInterpretationInput input, String text) {
    boolean mentionsInvoice = text.contains("factura");
    boolean mentionsReceipt = text.contains("boleta");
    InvoiceDocumentType documentType = mentionsInvoice
        ? InvoiceDocumentType.INVOICE
        : mentionsReceipt ? InvoiceDocumentType.SALES_RECEIPT
            : input.context().documentType();
    Matcher dni = DNI.matcher(text);
    Matcher ruc = RUC.matcher(text);
    boolean mentionsDni = dni.find();
    boolean mentionsRuc = ruc.find();
    IdentityDocumentType identityType = mentionsDni ? IdentityDocumentType.DNI
        : mentionsRuc ? IdentityDocumentType.RUC : input.context().recipientDocumentType();
    String documentNumber = null;
    if (identityType == IdentityDocumentType.DNI) {
      dni.reset();
      documentNumber = dni.find() ? dni.group(1) : input.context().recipientDocumentNumber();
    } else if (identityType == IdentityDocumentType.RUC) {
      ruc.reset();
      documentNumber = ruc.find() ? ruc.group(1) : input.context().recipientDocumentNumber();
    }
    String currency = detectCurrency(text, input.context().currency());
    List<String> missing = new ArrayList<>();
    if (documentType == null) {
      missing.add("documentType");
    }
    if (identityType == null) {
      missing.add("recipientDocumentType");
    }
    if (documentNumber == null) {
      missing.add("recipientDocumentNumber");
    }
    List<String> warnings = new ArrayList<>();
    List<String> ambiguous = new ArrayList<>();
    if (mentionsInvoice && mentionsReceipt) ambiguous.add("documentType");
    if (mentionsDni && mentionsRuc) ambiguous.add("recipientDocument");
    if (documentType == InvoiceDocumentType.INVOICE && identityType == IdentityDocumentType.DNI) {
      warnings.add("Una factura requiere un receptor con RUC");
    }
    return new DocumentInterpretation(InterpretationSource.TEXT,
        InterpretationIntent.START_DOCUMENT, documentType, identityType, documentNumber,
        currency, List.of(), null, confidence(missing, ambiguous), missing, ambiguous,
        List.of(), warnings);
  }

  private DocumentInterpretation interpretItem(TextInterpretationInput input, String text) {
    String working = text.replaceFirst("^(?:agrega|agregar|anade|anadir)\\s+", "");
    BigDecimal reportedTotal = null;
    Matcher totalMatcher = TOTAL.matcher(working);
    if (totalMatcher.find()) {
      reportedTotal = SpanishNumberParser.parse(totalMatcher.group(1)).orElse(null);
      working = working.substring(0, totalMatcher.start()).strip();
    }

    List<String> segments = splitItemSegments(working);
    List<String> missing = new ArrayList<>();
    List<String> warnings = new ArrayList<>();
    List<InterpretedInvoiceItem> items = new ArrayList<>();
    BigDecimal calculated = BigDecimal.ZERO;
    boolean calculationComplete = true;
    for (int index = 0; index < segments.size(); index++) {
      ParsedItem parsed = parseItemSegment(segments.get(index), index);
      missing.addAll(parsed.missingFields());
      warnings.addAll(parsed.warnings());
      BigDecimal itemReportedTotal = segments.size() == 1 ? reportedTotal : null;
      items.add(new InterpretedInvoiceItem(parsed.description(), UnitCode.NIU,
          parsed.quantity(), parsed.unitPrice(), BigDecimal.ZERO, TaxAffectation.TAXED,
          itemReportedTotal, confidence(parsed.missingFields(), List.of()), parsed.warnings()));
      if (parsed.quantity() == null || parsed.unitPrice() == null) {
        calculationComplete = false;
      } else {
        calculated = calculated.add(parsed.quantity().multiply(parsed.unitPrice()));
      }
    }

    List<String> calculationErrors = new ArrayList<>();
    if (reportedTotal != null && calculationComplete) {
      BigDecimal rounded = calculated.setScale(2, RoundingMode.HALF_UP);
      if (rounded.compareTo(reportedTotal.setScale(2, RoundingMode.HALF_UP)) != 0) {
        calculationErrors.add("El total indicado " + reportedTotal.toPlainString()
            + " no coincide con el total calculado " + rounded.toPlainString());
      }
    }
    return new DocumentInterpretation(InterpretationSource.TEXT, InterpretationIntent.ADD_ITEM,
        input.context().documentType(), input.context().recipientDocumentType(),
        input.context().recipientDocumentNumber(), input.context().currency(), items,
        reportedTotal, confidence(missing, List.of()), missing, List.of(), calculationErrors,
        warnings);
  }

  private ParsedItem parseItemSegment(String segment, int index) {
    segment = segment.replaceFirst("^(?:agrega|agregar|anade|anadir)\\s+", "");
    Matcher marker = PRICE_MARKER.matcher(segment);
    String beforePrice = segment;
    BigDecimal unitPrice = null;
    if (marker.find()) {
      beforePrice = segment.substring(0, marker.start()).strip();
      unitPrice = SpanishNumberParser.parse(segment.substring(marker.end())).orElse(null);
    }

    String description;
    BigDecimal quantity = null;
    int comma = beforePrice.indexOf(',');
    if (comma >= 0) {
      description = cleanDescription(beforePrice.substring(0, comma));
      String rawQuantity = beforePrice.substring(comma + 1)
          .replaceFirst("[,;:.\\s]+$", "");
      String quantityText = UNIT_SUFFIX.matcher(rawQuantity).replaceFirst("");
      quantity = SpanishNumberParser.parse(quantityText).orElse(null);
    } else {
      SpanishNumberParser.PrefixNumber prefix = SpanishNumberParser.parsePrefix(beforePrice);
      if (prefix != null) {
        quantity = prefix.value();
        description = cleanDescription(prefix.remainder()
            .replaceFirst("^(?:unidades?|unds?\\.?|u\\.)\\s+", ""));
      } else {
        description = cleanDescription(beforePrice);
      }
    }

    List<String> missing = new ArrayList<>();
    if (description == null || description.isBlank()) {
      missing.add("items[" + index + "].description");
      description = "Producto por confirmar";
    }
    if (quantity == null) missing.add("items[" + index + "].quantity");
    if (unitPrice == null) missing.add("items[" + index + "].unitPrice");
    return new ParsedItem(description, quantity, unitPrice, missing, List.of());
  }

  private List<String> splitItemSegments(String text) {
    List<String> result = new ArrayList<>();
    for (String block : text.split("\\s*(?:;|\\n+)\\s*")) {
      if (block.isBlank()) continue;
      String[] commaCandidates = ITEM_COMMA_SEPARATOR.split(block);
      if (commaCandidates.length > 1
          && java.util.Arrays.stream(commaCandidates).allMatch(this::hasItemShape)) {
        java.util.Arrays.stream(commaCandidates).map(String::strip)
            .filter(value -> !value.isBlank()).forEach(result::add);
      } else {
        result.add(block.strip());
      }
    }
    return result.isEmpty() ? List.of(text.strip()) : result;
  }

  private boolean hasItemShape(String value) {
    ParsedItem parsed = parseItemSegment(value, 0);
    return parsed.quantity() != null && !"Producto por confirmar".equals(parsed.description());
  }

  private boolean looksLikeHeader(String text) {
    return text.contains("boleta") || text.contains("factura")
        || DNI.matcher(text).find() || RUC.matcher(text).find();
  }

  private boolean looksLikeItem(String text) {
    return text.matches("^(?:agrega|agregar|anade|anadir)\\b.*")
        || PRICE_MARKER.matcher(text).find() || text.contains("precio unitario")
        || splitItemSegments(text).size() > 1;
  }

  private String detectCurrency(String text, String current) {
    if (text.matches(".*\\b(?:usd|dolar(?:es)?)\\b.*")) {
      return "USD";
    }
    if (text.matches(".*\\b(?:pen|sol(?:es)?)\\b.*")) {
      return "PEN";
    }
    return current == null ? "PEN" : current;
  }

  private String cleanDescription(String value) {
    String cleaned = value.replaceAll("^[,;:.\\s]+|[,;:.\\s]+$", "").strip();
    return cleaned.isBlank() ? null : capitalize(cleaned);
  }

  private String capitalize(String value) {
    return value.substring(0, 1).toUpperCase(Locale.ROOT) + value.substring(1);
  }

  private BigDecimal confidence(List<String> missing, List<String> ambiguous) {
    if (!ambiguous.isEmpty()) return new BigDecimal("0.40");
    return missing.isEmpty() ? new BigDecimal("0.95") : new BigDecimal("0.60");
  }

  private String normalize(String value) {
    return Normalizer.normalize(value.strip().toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
        .replaceAll("\\p{M}", "")
        .replaceAll("[\\t\\x0B\\f\\r ]+", " ")
        .replaceAll(" *\\n+ *", "\n");
  }

  private record ParsedItem(
      String description, BigDecimal quantity, BigDecimal unitPrice,
      List<String> missingFields, List<String> warnings) {}
}
