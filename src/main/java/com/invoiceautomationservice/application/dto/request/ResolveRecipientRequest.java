package com.invoiceautomationservice.application.dto.request;

import com.invoiceautomationservice.domain.model.IdentityDocumentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ResolveRecipientRequest(
    @NotNull IdentityDocumentType documentType,
    @NotBlank String documentNumber
) {
}
