package com.invoiceautomationservice.application.dto.request;

import com.invoiceautomationservice.domain.model.MessageDirection;
import com.invoiceautomationservice.domain.model.MessageStatus;
import com.invoiceautomationservice.domain.model.MessageType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateMessageRequest(
    @NotNull MessageDirection direction,
    @NotNull MessageType type,
    @Size(max = 4000) String content,
    @Size(max = 1000) String mediaUrl,
    @Size(max = 255) String externalMessageId,
    @NotNull MessageStatus status
) {}
