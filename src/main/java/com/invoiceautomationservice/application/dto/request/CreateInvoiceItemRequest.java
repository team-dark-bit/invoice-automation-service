package com.invoiceautomationservice.application.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record CreateInvoiceItemRequest(
        @NotBlank(message = "The field description must not be null or empty")
        String description,
        @NotNull(message = "The field quantity must not be null")
        @DecimalMin(value = "0.01", message = "The field quantity must be greater than zero")
        @Digits(integer = 12, fraction = 4, message = "The field quantity has an invalid decimal format")
        BigDecimal quantity,
        @NotNull(message = "The field unitPrice must not be null")
        @DecimalMin(value = "0.00", message = "The field unitPrice must not be negative")
        @Digits(integer = 14, fraction = 2, message = "The field unitPrice has an invalid decimal format")
        BigDecimal unitPrice
) {
}
