package com.invoiceautomationservice.application.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import java.util.List;

public record CreateInvoiceDraftRequest(
        @NotBlank(message = "The field companyId must not be null or empty")
        String companyId,
        @NotBlank(message = "The field customerId must not be null or empty")
        String customerId,
        @NotBlank(message = "The field currency must not be null or empty")
        @Pattern(regexp = "[A-Z]{3}", message = "The field currency must be a three-letter ISO code")
        String currency,
        @NotEmpty(message = "The invoice draft must contain at least one item")
        List<@Valid CreateInvoiceItemRequest> items
) {
}
