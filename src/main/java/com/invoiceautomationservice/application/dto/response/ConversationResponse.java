package com.invoiceautomationservice.application.dto.response;

import com.invoiceautomationservice.domain.model.ConversationChannel;
import com.invoiceautomationservice.domain.model.ConversationStatus;
import java.time.Instant;
import java.util.UUID;

public record ConversationResponse(
    UUID id, String companyId, String customerId, UUID invoiceDraftId,
    ConversationChannel channel, String externalParticipantId, ConversationStatus status,
    Instant createdAt, Instant updatedAt, Instant closedAt
) {}
