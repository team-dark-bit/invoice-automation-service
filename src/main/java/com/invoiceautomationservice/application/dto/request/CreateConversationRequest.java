package com.invoiceautomationservice.application.dto.request;

import com.invoiceautomationservice.domain.model.ConversationChannel;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CreateConversationRequest(
    String customerId,
    UUID invoiceDraftId,
    @NotNull ConversationChannel channel,
    @Size(max = 150) String externalParticipantId
) {}
