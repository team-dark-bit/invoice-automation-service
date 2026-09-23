package com.invoiceautomationservice.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record Conversation(
    UUID id, String companyId, String customerId, UUID invoiceDraftId,
    ConversationChannel channel, String externalParticipantId, ConversationStatus status,
    Instant createdAt, Instant updatedAt, Instant closedAt
) {
  public Conversation {
    Objects.requireNonNull(id, "id is required");
    Objects.requireNonNull(companyId, "companyId is required");
    Objects.requireNonNull(channel, "channel is required");
    Objects.requireNonNull(status, "status is required");
    Objects.requireNonNull(createdAt, "createdAt is required");
    Objects.requireNonNull(updatedAt, "updatedAt is required");
    externalParticipantId = normalizeNullable(externalParticipantId);
    if (status == ConversationStatus.CLOSED && closedAt == null) {
      throw new IllegalArgumentException("closed conversation requires closedAt");
    }
    if (status == ConversationStatus.OPEN && closedAt != null) {
      throw new IllegalArgumentException("open conversation cannot have closedAt");
    }
  }

  public static Conversation open(String companyId, String customerId, UUID invoiceDraftId,
      ConversationChannel channel, String externalParticipantId, Instant now) {
    return new Conversation(UUID.randomUUID(), companyId, customerId, invoiceDraftId, channel,
        externalParticipantId, ConversationStatus.OPEN, now, now, null);
  }

  public Conversation touch(Instant now) {
    ensureOpen();
    return new Conversation(id, companyId, customerId, invoiceDraftId, channel,
        externalParticipantId, status, createdAt, now, null);
  }

  public Conversation close(Instant now) {
    ensureOpen();
    return new Conversation(id, companyId, customerId, invoiceDraftId, channel,
        externalParticipantId, ConversationStatus.CLOSED, createdAt, now, now);
  }

  public void ensureOpen() {
    if (status != ConversationStatus.OPEN) {
      throw new IllegalStateException("conversation is closed");
    }
  }

  private static String normalizeNullable(String value) {
    return value == null || value.isBlank() ? null : value.strip();
  }
}
