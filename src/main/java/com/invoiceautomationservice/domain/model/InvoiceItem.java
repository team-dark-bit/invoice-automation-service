package com.invoiceautomationservice.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import java.util.UUID;

public record InvoiceItem(
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

  public InvoiceItem {
    Objects.requireNonNull(id, "id is required");
    Objects.requireNonNull(description, "description is required");
    Objects.requireNonNull(unitCode, "unitCode is required");
    Objects.requireNonNull(quantity, "quantity is required");
    Objects.requireNonNull(unitPrice, "unitPrice is required");
    Objects.requireNonNull(discount, "discount is required");
    Objects.requireNonNull(taxAffectation, "taxAffectation is required");
    Objects.requireNonNull(taxRate, "taxRate is required");
    Objects.requireNonNull(grossAmount, "grossAmount is required");
    Objects.requireNonNull(taxableAmount, "taxableAmount is required");
    Objects.requireNonNull(taxAmount, "taxAmount is required");
    Objects.requireNonNull(lineTotal, "lineTotal is required");
    if (description.isBlank()) {
      throw new IllegalArgumentException("description must not be blank");
    }
    if (quantity.signum() <= 0) {
      throw new IllegalArgumentException("quantity must be greater than zero");
    }
    if (unitPrice.signum() < 0) {
      throw new IllegalArgumentException("unitPrice must not be negative");
    }
    if (discount.signum() < 0 || discount.compareTo(grossAmount) > 0) {
      throw new IllegalArgumentException("discount must be between zero and gross amount");
    }
  }

  public static InvoiceItem create(
      String description,
      UnitCode unitCode,
      BigDecimal quantity,
      BigDecimal unitPrice,
      BigDecimal discount,
      TaxAffectation taxAffectation) {
    BigDecimal appliedDiscount = discount == null ? BigDecimal.ZERO : money(discount);
    BigDecimal grossAmount = money(quantity.multiply(unitPrice));
    BigDecimal netAmount = money(grossAmount.subtract(appliedDiscount));
    BigDecimal taxableAmount = taxAffectation == TaxAffectation.TAXED
        ? netAmount : BigDecimal.ZERO.setScale(2);
    BigDecimal taxAmount = money(
        taxableAmount.multiply(taxAffectation.taxRate()).divide(new BigDecimal("100")));
    return new InvoiceItem(
        UUID.randomUUID(), description.strip(), unitCode, quantity, money(unitPrice), appliedDiscount,
        taxAffectation, taxAffectation.taxRate(), grossAmount, taxableAmount, taxAmount,
        money(netAmount.add(taxAmount))
    );
  }

  public static InvoiceItem create(String description, BigDecimal quantity, BigDecimal unitPrice) {
    return create(description, UnitCode.NIU, quantity, unitPrice, BigDecimal.ZERO,
        TaxAffectation.UNAFFECTED);
  }

  private static BigDecimal money(BigDecimal value) {
    return value.setScale(2, RoundingMode.HALF_UP);
  }
}
