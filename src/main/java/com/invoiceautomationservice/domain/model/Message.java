package com.invoiceautomationservice.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record Message(
    UUID id, UUID conversationId, MessageDirection direction, MessageType type,
    String content, String mediaUrl, String externalMessageId, MessageStatus status,
    Instant createdAt
) {
  public Message {
    Objects.requireNonNull(id, "id is required");
    Objects.requireNonNull(conversationId, "conversationId is required");
    Objects.requireNonNull(direction, "direction is required");
    Objects.requireNonNull(type, "type is required");
    Objects.requireNonNull(status, "status is required");
    Objects.requireNonNull(createdAt, "createdAt is required");
    content = normalizeNullable(content);
    mediaUrl = normalizeNullable(mediaUrl);
    externalMessageId = normalizeNullable(externalMessageId);
    if (content == null && mediaUrl == null) {
      throw new IllegalArgumentException("message requires content or mediaUrl");
    }
    if (type == MessageType.TEXT && content == null) {
      throw new IllegalArgumentException("text message requires content");
    }
    if (direction == MessageDirection.INBOUND && status != MessageStatus.RECEIVED
        && status != MessageStatus.READ) {
      throw new IllegalArgumentException("inbound message status must be RECEIVED or READ");
    }
    if (direction == MessageDirection.OUTBOUND && status == MessageStatus.RECEIVED) {
      throw new IllegalArgumentException("outbound message cannot have RECEIVED status");
    }
  }

  public static Message create(UUID conversationId, MessageDirection direction, MessageType type,
      String content, String mediaUrl, String externalMessageId, MessageStatus status, Instant now) {
    return new Message(UUID.randomUUID(), conversationId, direction, type, content, mediaUrl,
        externalMessageId, status, now);
  }

  private static String normalizeNullable(String value) {
    return value == null || value.isBlank() ? null : value.strip();
  }
}
