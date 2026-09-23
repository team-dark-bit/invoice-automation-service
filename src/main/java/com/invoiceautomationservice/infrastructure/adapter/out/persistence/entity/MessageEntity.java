package com.invoiceautomationservice.infrastructure.adapter.out.persistence.entity;

import com.invoiceautomationservice.domain.model.MessageDirection;
import com.invoiceautomationservice.domain.model.MessageStatus;
import com.invoiceautomationservice.domain.model.MessageType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "messages")
public class MessageEntity {
  @Id private UUID id;
  @Column(name = "conversation_id", nullable = false) private UUID conversationId;
  @Enumerated(EnumType.STRING) @Column(nullable = false) private MessageDirection direction;
  @Enumerated(EnumType.STRING) @Column(nullable = false) private MessageType type;
  @Column(length = 4000) private String content;
  @Column(name = "media_url", length = 1000) private String mediaUrl;
  @Column(name = "external_message_id", length = 255) private String externalMessageId;
  @Enumerated(EnumType.STRING) @Column(nullable = false) private MessageStatus status;
  @Column(name = "created_at", nullable = false) private Instant createdAt;
}
