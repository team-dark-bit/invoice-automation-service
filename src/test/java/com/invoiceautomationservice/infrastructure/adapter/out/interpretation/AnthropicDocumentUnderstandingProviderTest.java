package com.invoiceautomationservice.infrastructure.adapter.out.interpretation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.invoiceautomationservice.domain.model.ImageInterpretationInput;
import com.invoiceautomationservice.domain.model.InterpretationContextSnapshot;
import com.invoiceautomationservice.domain.model.InterpretationIntent;
import com.invoiceautomationservice.domain.model.InterpretationSource;
import com.invoiceautomationservice.domain.model.InvoiceDocumentType;
import com.invoiceautomationservice.domain.model.TextInterpretationInput;
import com.invoiceautomationservice.infrastructure.config.AiProperties;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class AnthropicDocumentUnderstandingProviderTest {
  private JsonMapper jsonMapper;
  private AiProperties properties;

  @BeforeEach
  void setUp() {
    jsonMapper = JsonMapper.builder().findAndAddModules().build();
    properties = new AiProperties();
    properties.setModel("claude-test-model");
  }

  @Test
  void mapsStructuredVisionResponseToProviderNeutralDomainContract() throws Exception {
    var client = new CapturingClient(envelope("""
        {
          "intent":"ADD_ITEM",
          "documentType":"SALES_RECEIPT",
          "recipientDocumentType":"DNI",
          "recipientDocumentNumber":"12345678",
          "currency":"PEN",
          "items":[{
            "description":"Leche Gloria",
            "unitCode":"NIU",
            "quantity":2,
            "unitPrice":3.50,
            "discount":0,
            "reportedTotal":7.00,
            "confidence":0.97,
            "warnings":[]
          }],
          "reportedTotal":7.00,
          "confidence":0.96,
          "missingFields":[],
          "ambiguousFields":[],
          "calculationErrors":[],
          "warnings":[]
        }
        """));
    var provider = new AnthropicDocumentUnderstandingProvider(client, jsonMapper, properties);

    var result = provider.interpretImage(imageInput(new byte[] {1, 2, 3}));

    assertThat(result.source()).isEqualTo(InterpretationSource.IMAGE);
    assertThat(result.intent()).isEqualTo(InterpretationIntent.ADD_ITEM);
    assertThat(result.documentType()).isEqualTo(InvoiceDocumentType.SALES_RECEIPT);
    assertThat(result.confidence()).isEqualByComparingTo("0.96");
    assertThat(result.items()).singleElement().satisfies(item -> {
      assertThat(item.description()).isEqualTo("Leche Gloria");
      assertThat(item.quantity()).isEqualByComparingTo("2");
      assertThat(item.unitPrice()).isEqualByComparingTo("3.50");
      assertThat(item.taxAffectation()).isNull();
    });
    assertThat(client.request.get("model")).isEqualTo("claude-test-model");
    assertThat(client.request).containsKeys("messages", "output_config", "system");
  }

  @Test
  void keepsDeterministicRulesForTextWithoutCallingAnthropic() {
    var client = new CapturingClient("should not be used");
    var provider = new AnthropicDocumentUnderstandingProvider(client, jsonMapper, properties);

    var result = provider.interpretText(new TextInterpretationInput(
        "company-1", UUID.randomUUID(), "Dos leches a 3.50",
        InterpretationContextSnapshot.empty()));

    assertThat(result.intent()).isEqualTo(InterpretationIntent.ADD_ITEM);
    assertThat(client.request).isNull();
  }

  @Test
  void rejectsImagesThatExceedAnthropicBase64LimitBeforeCallingApi() {
    var client = new CapturingClient("should not be used");
    var provider = new AnthropicDocumentUnderstandingProvider(client, jsonMapper, properties);

    assertThatThrownBy(() -> provider.interpretImage(imageInput(new byte[7_864_321])))
        .isInstanceOf(DocumentUnderstandingProviderException.class)
        .hasMessageContaining("10 MB base64");
    assertThat(client.request).isNull();
  }

  @Test
  void rejectsMalformedProviderOutput() throws Exception {
    var client = new CapturingClient(envelope("{\"intent\":\"ADD_ITEM\"}"));
    var provider = new AnthropicDocumentUnderstandingProvider(client, jsonMapper, properties);

    assertThatThrownBy(() -> provider.interpretImage(imageInput(new byte[] {1})))
        .isInstanceOf(DocumentUnderstandingProviderException.class)
        .hasMessage("Anthropic returned an invalid interpretation");
  }

  private ImageInterpretationInput imageInput(byte[] content) {
    return new ImageInterpretationInput("company-1", UUID.randomUUID(), content,
        "image/png", "receipt.png", InterpretationContextSnapshot.empty());
  }

  private String envelope(String structuredOutput) throws Exception {
    return jsonMapper.writeValueAsString(Map.of(
        "content", java.util.List.of(Map.of("type", "text", "text", structuredOutput))));
  }

  private static final class CapturingClient implements AnthropicMessagesClient {
    private final String response;
    private Map<String, Object> request;

    private CapturingClient(String response) {
      this.response = response;
    }

    @Override
    public String createMessage(Map<String, Object> request) {
      this.request = request;
      return response;
    }
  }
}
