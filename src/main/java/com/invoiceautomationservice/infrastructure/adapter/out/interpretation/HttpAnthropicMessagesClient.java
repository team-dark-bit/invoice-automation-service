package com.invoiceautomationservice.infrastructure.adapter.out.interpretation;

import com.invoiceautomationservice.infrastructure.config.AiProperties;
import java.net.http.HttpClient;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
@ConditionalOnProperty(prefix = "ai", name = "provider", havingValue = "anthropic")
class HttpAnthropicMessagesClient implements AnthropicMessagesClient {
  private static final String API_VERSION = "2023-06-01";
  private final RestClient restClient;

  HttpAnthropicMessagesClient(RestClient.Builder builder, AiProperties properties) {
    if (properties.getApiKey() == null) {
      throw new IllegalStateException(
          "ANTHROPIC_API_KEY is required when AI_PROVIDER=anthropic");
    }
    var httpClient = HttpClient.newBuilder()
        .connectTimeout(properties.getConnectTimeout())
        .build();
    var requestFactory = new JdkClientHttpRequestFactory(httpClient);
    requestFactory.setReadTimeout(properties.getReadTimeout());
    this.restClient = builder.baseUrl(properties.getBaseUrl())
        .requestFactory(requestFactory)
        .defaultHeader("x-api-key", properties.getApiKey())
        .defaultHeader("anthropic-version", API_VERSION)
        .build();
  }

  @Override
  public String createMessage(Map<String, Object> request) {
    try {
      String response = restClient.post()
          .uri("/v1/messages")
          .contentType(MediaType.APPLICATION_JSON)
          .body(request)
          .retrieve()
          .body(String.class);
      if (response == null || response.isBlank()) {
        throw new DocumentUnderstandingProviderException(
            "Anthropic returned an empty response");
      }
      return response;
    } catch (RestClientResponseException exception) {
      throw new DocumentUnderstandingProviderException(
          "Anthropic request failed with HTTP " + exception.getStatusCode().value(), exception);
    } catch (ResourceAccessException exception) {
      throw new DocumentUnderstandingProviderException(
          "Anthropic request could not be completed", exception);
    }
  }
}
