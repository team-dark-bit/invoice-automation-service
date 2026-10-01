package com.invoiceautomationservice.infrastructure.adapter.out.persistence.entity;

import com.invoiceautomationservice.domain.model.ImageFormat;
import com.invoiceautomationservice.domain.model.ImageRetentionPolicy;
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
@Table(name = "conversation_images")
public class ConversationImageEntity {
  @Id private UUID id;
  @Column(name = "conversation_id", nullable = false) private UUID conversationId;
  @Column(name = "message_id", nullable = false, unique = true) private UUID messageId;
  @Column(name = "storage_key", nullable = false, unique = true, length = 500) private String storageKey;
  @Column(name = "original_filename", nullable = false, length = 255) private String originalFilename;
  @Enumerated(EnumType.STRING) @Column(nullable = false, length = 10) private ImageFormat format;
  @Column(name = "size_bytes", nullable = false) private long sizeBytes;
  @Column(nullable = false) private int width;
  @Column(nullable = false) private int height;
  @Column(nullable = false, length = 64) private String sha256;
  @Enumerated(EnumType.STRING)
  @Column(name = "retention_policy", nullable = false, length = 20)
  private ImageRetentionPolicy retentionPolicy;
  @Column(name = "expires_at") private Instant expiresAt;
  @Column(name = "created_at", nullable = false) private Instant createdAt;
  @Column(name = "deleted_at") private Instant deletedAt;
}
