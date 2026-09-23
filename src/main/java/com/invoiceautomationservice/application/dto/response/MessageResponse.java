package com.invoiceautomationservice.application.dto.response;

import com.invoiceautomationservice.domain.model.MessageDirection;
import com.invoiceautomationservice.domain.model.MessageStatus;
import com.invoiceautomationservice.domain.model.MessageType;
import java.time.Instant;
import java.util.UUID;

public record MessageResponse(
    UUID id, UUID conversationId, MessageDirection direction, MessageType type,
    String content, String mediaUrl, String externalMessageId, MessageStatus status,
    Instant createdAt
) {}
