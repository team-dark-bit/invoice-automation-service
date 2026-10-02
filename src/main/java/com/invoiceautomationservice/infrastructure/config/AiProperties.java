package com.invoiceautomationservice.infrastructure.config;

import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "ai")
public class AiProperties {
  private BigDecimal reviewThreshold = new BigDecimal("0.80");

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
}
