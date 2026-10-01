package com.invoiceautomationservice.infrastructure.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@ConfigurationProperties(prefix = "image-storage")
@Validated
public class ImageStorageProperties {
  @NotBlank
  private String root = "./data/images";
  @Positive
  private long maxSizeBytes = 10 * 1024 * 1024;
  @Positive
  private int maxWidth = 12000;
  @Positive
  private int maxHeight = 12000;
  @Positive
  private int defaultRetentionDays = 30;
  @Positive
  private int maxRetentionDays = 3650;

  public String getRoot() { return root; }
  public void setRoot(String root) { this.root = root; }
  public long getMaxSizeBytes() { return maxSizeBytes; }
  public void setMaxSizeBytes(long maxSizeBytes) { this.maxSizeBytes = maxSizeBytes; }
  public int getMaxWidth() { return maxWidth; }
  public void setMaxWidth(int maxWidth) { this.maxWidth = maxWidth; }
  public int getMaxHeight() { return maxHeight; }
  public void setMaxHeight(int maxHeight) { this.maxHeight = maxHeight; }
  public int getDefaultRetentionDays() { return defaultRetentionDays; }
  public void setDefaultRetentionDays(int defaultRetentionDays) {
    this.defaultRetentionDays = defaultRetentionDays;
  }
  public int getMaxRetentionDays() { return maxRetentionDays; }
  public void setMaxRetentionDays(int maxRetentionDays) {
    this.maxRetentionDays = maxRetentionDays;
  }
}
