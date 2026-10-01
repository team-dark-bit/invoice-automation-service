package com.invoiceautomationservice.application.model;

import com.invoiceautomationservice.domain.model.ImageRetentionPolicy;
import java.util.Arrays;

public record UploadConversationImageCommand(
    String originalFilename, String declaredContentType, byte[] content,
    String externalMessageId, ImageRetentionPolicy retentionPolicy, Integer retentionDays
) {
  public UploadConversationImageCommand {
    content = content == null ? null : Arrays.copyOf(content, content.length);
  }

  @Override
  public byte[] content() {
    return content == null ? null : Arrays.copyOf(content, content.length);
  }
}
