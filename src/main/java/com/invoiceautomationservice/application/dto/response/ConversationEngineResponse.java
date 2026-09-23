package com.invoiceautomationservice.application.dto.response;

import java.util.UUID;

public record ConversationEngineResponse(
    UUID conversationId, String reply, ConversationContextResponse context
) {}
