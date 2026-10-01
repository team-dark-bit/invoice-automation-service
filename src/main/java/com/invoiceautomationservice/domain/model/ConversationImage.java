package com.invoiceautomationservice.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record ConversationImage(
    UUID id, UUID conversationId, UUID messageId, String storageKey, String originalFilename,
    ImageFormat format, long sizeBytes, int width, int height, String sha256,
    ImageRetentionPolicy retentionPolicy, Instant expiresAt, Instant createdAt, Instant deletedAt
) {
  public ConversationImage {
    Objects.requireNonNull(id, "id is required");
    Objects.requireNonNull(conversationId, "conversationId is required");
    Objects.requireNonNull(messageId, "messageId is required");
    Objects.requireNonNull(storageKey, "storageKey is required");
    Objects.requireNonNull(originalFilename, "originalFilename is required");
    Objects.requireNonNull(format, "format is required");
    Objects.requireNonNull(sha256, "sha256 is required");
    Objects.requireNonNull(retentionPolicy, "retentionPolicy is required");
    Objects.requireNonNull(createdAt, "createdAt is required");
    if (storageKey.isBlank() || originalFilename.isBlank() || sizeBytes <= 0
        || width <= 0 || height <= 0 || !sha256.matches("[a-f0-9]{64}")) {
      throw new IllegalArgumentException("invalid conversation image metadata");
    }
    if (retentionPolicy == ImageRetentionPolicy.TEMPORARY && expiresAt == null) {
      throw new IllegalArgumentException("temporary image requires an expiration date");
    }
    if (retentionPolicy == ImageRetentionPolicy.PERMANENT && expiresAt != null) {
      throw new IllegalArgumentException("permanent image cannot have an expiration date");
    }
  }

  public boolean active() {
    return deletedAt == null;
  }

  public ConversationImage delete(Instant now) {
    Objects.requireNonNull(now, "deletion date is required");
    return new ConversationImage(id, conversationId, messageId, storageKey, originalFilename,
        format, sizeBytes, width, height, sha256, retentionPolicy, expiresAt, createdAt, now);
  }
}
