package com.invoiceautomationservice.application.dto.request;

import com.invoiceautomationservice.domain.model.InvoiceDocumentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record ConfigureDocumentSeriesRequest(
    @NotNull InvoiceDocumentType documentType,
    @NotBlank @Pattern(regexp = "[FB][A-Z0-9]{3}") String series
) {
}
