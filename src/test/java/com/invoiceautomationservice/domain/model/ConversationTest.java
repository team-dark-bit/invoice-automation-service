package com.invoiceautomationservice.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class ConversationTest {
  @Test
  void closesOpenConversationAndKeepsCreationDate() {
    Instant createdAt = Instant.parse("2026-09-23T10:00:00Z");
    Instant closedAt = Instant.parse("2026-09-23T11:00:00Z");
    Conversation conversation = Conversation.open(
        "company-1", null, null, ConversationChannel.REST, "contact-1", createdAt);

    Conversation closed = conversation.close(closedAt);

    assertThat(closed.status()).isEqualTo(ConversationStatus.CLOSED);
    assertThat(closed.createdAt()).isEqualTo(createdAt);
    assertThat(closed.updatedAt()).isEqualTo(closedAt);
    assertThat(closed.closedAt()).isEqualTo(closedAt);
  }

  @Test
  void closedConversationCannotReceiveMoreActivity() {
    Conversation closed = Conversation.open("company-1", null, null,
        ConversationChannel.REST, null, Instant.parse("2026-09-23T10:00:00Z"))
        .close(Instant.parse("2026-09-23T11:00:00Z"));

    assertThatThrownBy(() -> closed.touch(Instant.parse("2026-09-23T12:00:00Z")))
        .isInstanceOf(IllegalStateException.class).hasMessageContaining("closed");
  }
}
