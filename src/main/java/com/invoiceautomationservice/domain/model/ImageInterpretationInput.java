package com.invoiceautomationservice.domain.model;

import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;

public final class ImageInterpretationInput {
  private final String companyId;
  private final UUID conversationId;
  private final byte[] content;
  private final String mimeType;
  private final String fileName;
  private final InterpretationContextSnapshot context;

  public ImageInterpretationInput(
      String companyId,
      UUID conversationId,
      byte[] content,
      String mimeType,
      String fileName,
      InterpretationContextSnapshot context) {
    this.companyId = requireText(companyId, "companyId");
    this.conversationId = Objects.requireNonNull(
        conversationId, "conversationId must not be null");
    if (content == null || content.length == 0) {
      throw new IllegalArgumentException("content must not be empty");
    }
    this.content = Arrays.copyOf(content, content.length);
    this.mimeType = requireText(mimeType, "mimeType").toLowerCase(java.util.Locale.ROOT);
    this.fileName = requireText(fileName, "fileName");
    this.context = Objects.requireNonNull(context, "context must not be null");
  }

  public String companyId() {
    return companyId;
  }

  public UUID conversationId() {
    return conversationId;
  }

  public byte[] content() {
    return Arrays.copyOf(content, content.length);
  }

  public String mimeType() {
    return mimeType;
  }

  public String fileName() {
    return fileName;
  }

  public InterpretationContextSnapshot context() {
    return context;
  }

  private static String requireText(String value, String field) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(field + " must not be blank");
    }
    return value.strip();
  }
}
