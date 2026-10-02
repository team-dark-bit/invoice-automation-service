package com.invoiceautomationservice.application.dto.response;

import com.invoiceautomationservice.domain.model.ImageRetentionPolicy;
import com.invoiceautomationservice.domain.model.ImageProcessingStatus;
import com.invoiceautomationservice.domain.model.DocumentInterpretation;
import java.time.Instant;
import java.util.UUID;

public record ConversationImageResponse(
    UUID id, UUID conversationId, UUID messageId, String originalFilename, String contentType,
    long sizeBytes, int width, int height, String sha256, ImageRetentionPolicy retentionPolicy,
    Instant expiresAt, Instant createdAt, Instant deletedAt, boolean duplicate,
    ImageProcessingStatus processingStatus, int processingAttempts,
    Instant processingStartedAt, Instant processedAt, String lastProcessingError,
    DocumentInterpretation interpretation
) {}
