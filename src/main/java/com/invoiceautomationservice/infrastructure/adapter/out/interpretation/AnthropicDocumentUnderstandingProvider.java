package com.invoiceautomationservice.infrastructure.adapter.out.interpretation;

import com.invoiceautomationservice.application.port.out.DocumentUnderstandingProvider;
import com.invoiceautomationservice.domain.model.DocumentInterpretation;
import com.invoiceautomationservice.domain.model.IdentityDocumentType;
import com.invoiceautomationservice.domain.model.ImageInterpretationInput;
import com.invoiceautomationservice.domain.model.InterpretationIntent;
import com.invoiceautomationservice.domain.model.InterpretationSource;
import com.invoiceautomationservice.domain.model.InterpretedInvoiceItem;
import com.invoiceautomationservice.domain.model.InvoiceDocumentType;
import com.invoiceautomationservice.domain.model.TextInterpretationInput;
import com.invoiceautomationservice.domain.model.UnitCode;
import com.invoiceautomationservice.infrastructure.config.AiProperties;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Anthropic vision adapter. The application and domain only depend on the neutral output port. */
@Component
@ConditionalOnProperty(prefix = "ai", name = "provider", havingValue = "anthropic")
public class AnthropicDocumentUnderstandingProvider implements DocumentUnderstandingProvider {
  private static final long MAX_BASE64_IMAGE_BYTES = 10L * 1024 * 1024;
  private static final String SYSTEM_PROMPT = """
      Eres un extractor de datos para comprobantes peruanos. Analiza únicamente lo visible en la
      imagen y devuelve el contrato JSON solicitado. No inventes valores. Usa null y missingFields
      cuando un dato no sea legible. La confianza va de 0 a 1. No decidas impuestos, IGV,
      correlativos, aprobación ni emisión. reportedTotal es solo el total escrito en la imagen.
      Extrae todos los productos visibles: crea un elemento de items por cada fila o producto y no
      combines productos diferentes. Los precios y descuentos son candidatos y serán recalculados
      por el dominio.
      """;
  private static final String OUTPUT_SCHEMA = """
      {
        "type":"object",
        "additionalProperties":false,
        "required":["intent","documentType","recipientDocumentType","recipientDocumentNumber",
          "currency","items","reportedTotal","confidence","missingFields","ambiguousFields",
          "calculationErrors","warnings"],
        "properties":{
          "intent":{"type":"string","enum":["START_DOCUMENT","ADD_ITEM","REVIEW","UNKNOWN"]},
          "documentType":{"anyOf":[
            {"type":"string","enum":["INVOICE","SALES_RECEIPT","CREDIT_NOTE","DEBIT_NOTE"]},
            {"type":"null"}]},
          "recipientDocumentType":{"anyOf":[
            {"type":"string","enum":["DNI","RUC"]},{"type":"null"}]},
          "recipientDocumentNumber":{"type":["string","null"]},
          "currency":{"type":["string","null"]},
          "items":{"type":"array","items":{"type":"object","additionalProperties":false,
            "required":["description","unitCode","quantity","unitPrice","discount",
              "reportedTotal","confidence","warnings"],
            "properties":{
              "description":{"type":"string"},
              "unitCode":{"anyOf":[
                {"type":"string","enum":["NIU","ZZ","KGM","LTR"]},{"type":"null"}]},
              "quantity":{"type":["number","null"]},
              "unitPrice":{"type":["number","null"]},
              "discount":{"type":["number","null"]},
              "reportedTotal":{"type":["number","null"]},
              "confidence":{"type":"number"},
              "warnings":{"type":"array","items":{"type":"string"}}
            }}},
          "reportedTotal":{"type":["number","null"]},
          "confidence":{"type":"number"},
          "missingFields":{"type":"array","items":{"type":"string"}},
          "ambiguousFields":{"type":"array","items":{"type":"string"}},
          "calculationErrors":{"type":"array","items":{"type":"string"}},
          "warnings":{"type":"array","items":{"type":"string"}}
        }
      }
      """;

  private final AnthropicMessagesClient client;
  private final JsonMapper jsonMapper;
  private final AiProperties properties;
  private final RuleBasedDocumentUnderstandingProvider textProvider =
      new RuleBasedDocumentUnderstandingProvider();

  AnthropicDocumentUnderstandingProvider(
      AnthropicMessagesClient client, JsonMapper jsonMapper, AiProperties properties) {
    this.client = Objects.requireNonNull(client, "client must not be null");
    this.jsonMapper = Objects.requireNonNull(jsonMapper, "jsonMapper must not be null");
    this.properties = Objects.requireNonNull(properties, "properties must not be null");
  }

  @Override
  public DocumentInterpretation interpretText(TextInterpretationInput input) {
    return textProvider.interpretText(input);
  }

  @Override
  public DocumentInterpretation interpretImage(ImageInterpretationInput input) {
    Objects.requireNonNull(input, "input must not be null");
    validateEncodedSize(input.content().length);
    try {
      Map<String, Object> request = createRequest(input);
      JsonNode envelope = jsonMapper.readTree(client.createMessage(request));
      String structuredJson = extractText(envelope);
      return toDomain(jsonMapper.readTree(structuredJson));
    } catch (DocumentUnderstandingProviderException exception) {
      throw exception;
    } catch (Exception exception) {
      throw new DocumentUnderstandingProviderException(
          "Anthropic returned an invalid interpretation", exception);
    }
  }

  private Map<String, Object> createRequest(ImageInterpretationInput input) throws Exception {
    Map<String, Object> imageSource = Map.of(
        "type", "base64",
        "media_type", input.mimeType(),
        "data", Base64.getEncoder().encodeToString(input.content()));
    Map<String, Object> image = Map.of("type", "image", "source", imageSource);
    String context = jsonMapper.writeValueAsString(input.context());
    Map<String, Object> instruction = Map.of("type", "text", "text",
        "Extrae los datos visibles. Contexto ya confirmado de la conversación: " + context
            + ". No reemplaces valores confirmados salvo evidencia explícita en la imagen.");
    Map<String, Object> message = Map.of(
        "role", "user", "content", List.of(image, instruction));
    Map<String, Object> format = Map.of(
        "type", "json_schema", "schema", jsonMapper.readValue(OUTPUT_SCHEMA, Map.class));

    var request = new LinkedHashMap<String, Object>();
    request.put("model", properties.getModel());
    request.put("max_tokens", properties.getMaxTokens());
    request.put("system", SYSTEM_PROMPT);
    request.put("messages", List.of(message));
    request.put("output_config", Map.of("format", format));
    return request;
  }

  private String extractText(JsonNode envelope) {
    JsonNode content = envelope.path("content");
    if (!content.isArray()) {
      throw new DocumentUnderstandingProviderException(
          "Anthropic response does not contain content");
    }
    for (JsonNode block : content) {
      if ("text".equals(block.path("type").asText()) && !block.path("text").asText().isBlank()) {
        return block.path("text").asText();
      }
    }
    throw new DocumentUnderstandingProviderException(
        "Anthropic response does not contain structured text");
  }

  private DocumentInterpretation toDomain(JsonNode value) {
    List<String> warnings = strings(value.path("warnings"));
    List<InterpretedInvoiceItem> items = new ArrayList<>();
    for (JsonNode item : value.path("items")) {
      UnitCode unit = enumValue(UnitCode.class, nullableText(item, "unitCode"));
      if (unit == null) {
        unit = UnitCode.NIU;
        warnings.add("Unidad no detectada; se propone NIU para revisión");
      }
      items.add(new InterpretedInvoiceItem(
          requiredText(item, "description"), unit, decimal(item, "quantity"),
          decimal(item, "unitPrice"), decimalOrZero(item, "discount"), null,
          decimal(item, "reportedTotal"), requiredDecimal(item, "confidence"),
          strings(item.path("warnings"))));
    }
    return new DocumentInterpretation(InterpretationSource.IMAGE,
        requiredEnum(InterpretationIntent.class, value, "intent"),
        enumValue(InvoiceDocumentType.class, nullableText(value, "documentType")),
        enumValue(IdentityDocumentType.class, nullableText(value, "recipientDocumentType")),
        nullableText(value, "recipientDocumentNumber"), nullableText(value, "currency"), items,
        decimal(value, "reportedTotal"), requiredDecimal(value, "confidence"),
        strings(value.path("missingFields")), strings(value.path("ambiguousFields")),
        strings(value.path("calculationErrors")), warnings);
  }

  private void validateEncodedSize(int rawBytes) {
    long encodedBytes = 4L * ((rawBytes + 2L) / 3L);
    if (encodedBytes > MAX_BASE64_IMAGE_BYTES) {
      throw new DocumentUnderstandingProviderException(
          "Image exceeds Anthropic's 10 MB base64 limit; resize or compress it");
    }
  }

  private String requiredText(JsonNode node, String field) {
    String value = nullableText(node, field);
    if (value == null) throw new IllegalArgumentException(field + " is required");
    return value;
  }

  private String nullableText(JsonNode node, String field) {
    JsonNode value = node.path(field);
    return value.isMissingNode() || value.isNull() || value.asText().isBlank()
        ? null : value.asText().strip();
  }

  private BigDecimal decimal(JsonNode node, String field) {
    JsonNode value = node.path(field);
    return value.isMissingNode() || value.isNull() ? null : value.decimalValue();
  }

  private BigDecimal requiredDecimal(JsonNode node, String field) {
    BigDecimal value = decimal(node, field);
    if (value == null) throw new IllegalArgumentException(field + " is required");
    return value;
  }

  private BigDecimal decimalOrZero(JsonNode node, String field) {
    BigDecimal value = decimal(node, field);
    return value == null ? BigDecimal.ZERO : value;
  }

  private List<String> strings(JsonNode node) {
    var values = new ArrayList<String>();
    if (!node.isArray()) return values;
    node.forEach(value -> {
      if (!value.asText().isBlank()) values.add(value.asText().strip());
    });
    return values;
  }

  private <E extends Enum<E>> E requiredEnum(Class<E> type, JsonNode node, String field) {
    E value = enumValue(type, nullableText(node, field));
    if (value == null) throw new IllegalArgumentException(field + " is required");
    return value;
  }

  private <E extends Enum<E>> E enumValue(Class<E> type, String value) {
    return value == null ? null : Enum.valueOf(type, value.toUpperCase(Locale.ROOT));
  }
}
