package com.invoiceautomationservice.application.dto.response;

import com.invoiceautomationservice.domain.model.ImageRetentionPolicy;
import java.time.Instant;
import java.util.UUID;

public record ConversationImageResponse(
    UUID id, UUID conversationId, UUID messageId, String originalFilename, String contentType,
    long sizeBytes, int width, int height, String sha256, ImageRetentionPolicy retentionPolicy,
    Instant expiresAt, Instant createdAt, Instant deletedAt, boolean duplicate
) {}
