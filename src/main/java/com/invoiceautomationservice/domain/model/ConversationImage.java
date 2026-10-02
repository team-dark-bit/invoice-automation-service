package com.invoiceautomationservice.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record ConversationImage(
    UUID id, UUID conversationId, UUID messageId, String storageKey, String originalFilename,
    ImageFormat format, long sizeBytes, int width, int height, String sha256,
    ImageRetentionPolicy retentionPolicy, Instant expiresAt, Instant createdAt, Instant deletedAt,
    ImageProcessingStatus processingStatus, int processingAttempts,
    Instant processingStartedAt, Instant processedAt, String lastProcessingError,
    DocumentInterpretation interpretation
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
    Objects.requireNonNull(processingStatus, "processingStatus is required");
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
    if (processingAttempts < 0) {
      throw new IllegalArgumentException("processingAttempts must not be negative");
    }
    lastProcessingError = normalizeOptional(lastProcessingError);
    validateProcessingState(processingStatus, processingAttempts, processingStartedAt,
        processedAt, lastProcessingError, interpretation);
  }

  public boolean active() {
    return deletedAt == null;
  }

  public ConversationImage delete(Instant now) {
    Objects.requireNonNull(now, "deletion date is required");
    return new ConversationImage(id, conversationId, messageId, storageKey, originalFilename,
        format, sizeBytes, width, height, sha256, retentionPolicy, expiresAt, createdAt, now,
        processingStatus, processingAttempts, processingStartedAt, processedAt,
        lastProcessingError, interpretation);
  }

  public ConversationImage startProcessing(Instant now) {
    if (!active()) throw new IllegalStateException("deleted image cannot be processed");
    if (processingStatus != ImageProcessingStatus.RECEIVED) {
      throw new IllegalStateException("image is not waiting for processing");
    }
    return copy(ImageProcessingStatus.PROCESSING, processingAttempts + 1, now, null, null, null);
  }

  public ConversationImage extracted(DocumentInterpretation result, Instant now) {
    if (processingStatus != ImageProcessingStatus.PROCESSING) {
      throw new IllegalStateException("image is not being processed");
    }
    Objects.requireNonNull(result, "interpretation is required");
    if (result.source() != InterpretationSource.IMAGE) {
      throw new IllegalArgumentException("image processing requires an IMAGE interpretation");
    }
    return copy(ImageProcessingStatus.EXTRACTED, processingAttempts, processingStartedAt,
        now, null, result);
  }

  public ConversationImage failProcessing(String error, Instant now) {
    if (processingStatus != ImageProcessingStatus.PROCESSING) {
      throw new IllegalStateException("image is not being processed");
    }
    String normalizedError = normalizeOptional(error);
    if (normalizedError == null) throw new IllegalArgumentException("processing error is required");
    return copy(ImageProcessingStatus.FAILED, processingAttempts, processingStartedAt,
        now, normalizedError, null);
  }

  public ConversationImage retry() {
    if (!active()) throw new IllegalStateException("deleted image cannot be processed");
    if (processingStatus != ImageProcessingStatus.FAILED) {
      throw new IllegalStateException("only failed image processing can be retried");
    }
    return copy(ImageProcessingStatus.RECEIVED, processingAttempts, null, null, null, null);
  }

  private ConversationImage copy(ImageProcessingStatus status, int attempts, Instant startedAt,
      Instant completedAt, String error, DocumentInterpretation result) {
    return new ConversationImage(id, conversationId, messageId, storageKey, originalFilename,
        format, sizeBytes, width, height, sha256, retentionPolicy, expiresAt, createdAt, deletedAt,
        status, attempts, startedAt, completedAt, error, result);
  }

  private static void validateProcessingState(ImageProcessingStatus status, int attempts,
      Instant startedAt, Instant completedAt, String error, DocumentInterpretation result) {
    boolean valid = switch (status) {
      case RECEIVED -> startedAt == null && completedAt == null
          && error == null && result == null;
      case PROCESSING -> startedAt != null && completedAt == null
          && error == null && result == null && attempts > 0;
      case EXTRACTED -> startedAt != null && completedAt != null
          && error == null && result != null && attempts > 0;
      case FAILED -> startedAt != null && completedAt != null
          && error != null && result == null && attempts > 0;
    };
    if (!valid) throw new IllegalArgumentException("invalid image processing state");
  }

  private static String normalizeOptional(String value) {
    return value == null || value.isBlank() ? null : value.strip();
  }
}
