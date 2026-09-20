package com.invoiceautomationservice.application.dto.response;

import java.math.BigDecimal;
import java.util.UUID;
import com.invoiceautomationservice.domain.model.TaxAffectation;
import com.invoiceautomationservice.domain.model.UnitCode;

public record InvoiceItemResponse(
        UUID id,
        String description,
        UnitCode unitCode,
        BigDecimal quantity,
        BigDecimal unitPrice,
        BigDecimal discount,
        TaxAffectation taxAffectation,
        BigDecimal taxRate,
        BigDecimal grossAmount,
        BigDecimal taxableAmount,
        BigDecimal taxAmount,
        BigDecimal lineTotal
) {
  public InvoiceItemResponse(
      UUID id, String description, BigDecimal quantity, BigDecimal unitPrice, BigDecimal lineTotal) {
    this(id, description, UnitCode.NIU, quantity, unitPrice, BigDecimal.ZERO,
        TaxAffectation.UNAFFECTED, BigDecimal.ZERO, lineTotal, BigDecimal.ZERO,
        BigDecimal.ZERO, lineTotal);
  }
}
