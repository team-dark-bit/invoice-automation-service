package com.invoiceautomationservice.application.model;

import com.invoiceautomationservice.domain.model.ImageFormat;
import java.util.Arrays;

public record ConversationImageContent(
    String filename, ImageFormat format, byte[] content
) {
  public ConversationImageContent {
    content = Arrays.copyOf(content, content.length);
  }

  @Override
  public byte[] content() {
    return Arrays.copyOf(content, content.length);
  }
}
