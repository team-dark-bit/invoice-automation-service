package com.invoiceautomationservice.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProcessConversationRequest(
    @NotBlank @Size(max = 4000) String text,
    @Size(max = 255) String externalMessageId
) {}
