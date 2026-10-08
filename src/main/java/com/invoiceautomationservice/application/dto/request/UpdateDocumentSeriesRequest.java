package com.invoiceautomationservice.application.dto.request;

import jakarta.validation.constraints.Pattern;

public record UpdateDocumentSeriesRequest(
    @Pattern(regexp = "[FB][A-Z0-9]{3}") String series,
    Boolean active
) {}
