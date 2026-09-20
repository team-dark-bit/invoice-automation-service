package com.invoiceautomationservice.application.dto.response;

import com.invoiceautomationservice.domain.model.TaxpayerType;
import java.time.Instant;

public record IssuerTaxProfileResponse(
    String companyId,
    TaxpayerType taxpayerType,
    String fiscalAddress,
    String ubigeo,
    String department,
    String province,
    String district,
    String countryCode,
    boolean completed,
    Instant createdAt,
    Instant updatedAt
) {
}
