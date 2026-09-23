package com.invoiceautomationservice.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MessageTest {
  @Test
  void createsNormalizedTextMessage() {
    Message message = Message.create(UUID.randomUUID(), MessageDirection.INBOUND,
        MessageType.TEXT, "  emitir boleta  ", null, "provider-1", MessageStatus.RECEIVED,
        Instant.parse("2026-09-23T10:00:00Z"));

    assertThat(message.content()).isEqualTo("emitir boleta");
    assertThat(message.status()).isEqualTo(MessageStatus.RECEIVED);
  }

  @Test
  void requiresPayloadAndTextContent() {
    UUID conversationId = UUID.randomUUID();
    Instant now = Instant.parse("2026-09-23T10:00:00Z");
    assertThatThrownBy(() -> Message.create(conversationId, MessageDirection.INBOUND,
        MessageType.TEXT, null, null, null, MessageStatus.RECEIVED, now))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
