package com.invoiceautomationservice.application.dto.response;

import com.invoiceautomationservice.domain.model.InvoiceDraftStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record InvoiceDraftResponse(
        UUID id,
        String companyId,
        String customerId,
        String currency,
        InvoiceDraftStatus status,
        List<InvoiceItemResponse> items,
        BigDecimal subtotal,
        BigDecimal total,
        Instant createdAt,
        Instant updatedAt
) {
}
