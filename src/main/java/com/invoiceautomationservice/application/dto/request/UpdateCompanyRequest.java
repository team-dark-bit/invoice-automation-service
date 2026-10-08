package com.invoiceautomationservice.application.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateCompanyRequest(
    @Size(min = 1, max = 150) String legalName,
    @Size(max = 150) String tradeName,
    @Pattern(regexp = "\\d{11}") String taxId,
    @Size(max = 250) String address,
    Boolean active
) {}
