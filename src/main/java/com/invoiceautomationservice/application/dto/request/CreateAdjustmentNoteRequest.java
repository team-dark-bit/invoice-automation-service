package com.invoiceautomationservice.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CreateAdjustmentNoteRequest(
    @NotBlank @Pattern(regexp = "\\d{2}") String reasonCode,
    @NotBlank String reason
) {
}
