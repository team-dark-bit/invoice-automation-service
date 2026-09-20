package com.invoiceautomationservice.application.dto.request;

import com.invoiceautomationservice.domain.model.TaxpayerType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record ConfigureIssuerTaxProfileRequest(
    @NotNull TaxpayerType taxpayerType,
    @NotBlank String fiscalAddress,
    @NotBlank @Pattern(regexp = "\\d{6}") String ubigeo,
    @NotBlank String department,
    @NotBlank String province,
    @NotBlank String district,
    @NotBlank @Pattern(regexp = "[A-Z]{2}") String countryCode
) {
}
