package com.invoiceautomationservice.infrastructure.config;

import java.math.BigDecimal;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "ai")
public class AiProperties {
  private BigDecimal reviewThreshold = new BigDecimal("0.80");
  private String model = "claude-sonnet-5-5";
  private String apiKey;
  private String baseUrl = "https://api.anthropic.com";
  private int maxTokens = 2048;
  private Duration connectTimeout = Duration.ofSeconds(10);
  private Duration readTimeout = Duration.ofSeconds(90);

  public BigDecimal getReviewThreshold() {
    return reviewThreshold;
  }

  public void setReviewThreshold(BigDecimal reviewThreshold) {
    if (reviewThreshold == null || reviewThreshold.compareTo(BigDecimal.ZERO) < 0
        || reviewThreshold.compareTo(BigDecimal.ONE) > 0) {
      throw new IllegalArgumentException("ai.review-threshold must be between zero and one");
    }
    this.reviewThreshold = reviewThreshold;
  }

  public String getModel() { return model; }
  public void setModel(String model) { this.model = requireText(model, "ai.model"); }
  public String getApiKey() { return apiKey; }
  public void setApiKey(String apiKey) {
    this.apiKey = apiKey == null || apiKey.isBlank() ? null : apiKey.strip();
  }
  public String getBaseUrl() { return baseUrl; }
  public void setBaseUrl(String baseUrl) {
    this.baseUrl = requireText(baseUrl, "ai.base-url");
  }
  public int getMaxTokens() { return maxTokens; }
  public void setMaxTokens(int maxTokens) {
    if (maxTokens <= 0) throw new IllegalArgumentException("ai.max-tokens must be positive");
    this.maxTokens = maxTokens;
  }
  public Duration getConnectTimeout() { return connectTimeout; }
  public void setConnectTimeout(Duration connectTimeout) {
    this.connectTimeout = requirePositive(connectTimeout, "ai.connect-timeout");
  }
  public Duration getReadTimeout() { return readTimeout; }
  public void setReadTimeout(Duration readTimeout) {
    this.readTimeout = requirePositive(readTimeout, "ai.read-timeout");
  }

  private static String requireText(String value, String field) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(field + " must not be blank");
    }
    return value.strip();
  }

  private static Duration requirePositive(Duration value, String field) {
    if (value == null || value.isZero() || value.isNegative()) {
      throw new IllegalArgumentException(field + " must be positive");
    }
    return value;
  }
}
