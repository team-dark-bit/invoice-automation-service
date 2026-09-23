package com.invoiceautomationservice.application.dto.request;

import com.invoiceautomationservice.domain.model.IdentityDocumentType;
import com.invoiceautomationservice.domain.model.InvoiceDocumentType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.List;

public record UpdateInvoiceDraftRequest(
    @NotNull(message = "The field documentType must not be null")
    InvoiceDocumentType documentType,
    @NotNull(message = "The field recipientDocumentType must not be null")
    IdentityDocumentType recipientDocumentType,
    @NotBlank(message = "The field recipientDocumentNumber must not be null or empty")
    String recipientDocumentNumber,
    @NotBlank(message = "The field currency must not be null or empty")
    @Pattern(regexp = "[A-Z]{3}",
        message = "The field currency must be a three-letter ISO code")
    String currency,
    @NotEmpty(message = "The invoice draft must contain at least one item")
    List<@Valid CreateInvoiceItemRequest> items
) {
}
