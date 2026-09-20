package com.invoiceautomationservice.infrastructure.adapter.out.persistence.entity;

import com.invoiceautomationservice.domain.model.TaxAffectation;
import com.invoiceautomationservice.domain.model.UnitCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "electronic_document_items")
public class ElectronicDocumentItemEntity {
  @Id private UUID id;
  @Column(name = "document_id", nullable = false) private UUID documentId;
  @Column(nullable = false) private Integer position;
  @Column(nullable = false) private String description;
  @Enumerated(EnumType.STRING) @Column(name = "unit_code", nullable = false) private UnitCode unitCode;
  @Column(nullable = false, precision = 16, scale = 4) private BigDecimal quantity;
  @Column(name = "unit_price", nullable = false, precision = 20, scale = 2) private BigDecimal unitPrice;
  @Column(nullable = false, precision = 20, scale = 2) private BigDecimal discount;
  @Enumerated(EnumType.STRING) @Column(name = "tax_affectation", nullable = false)
  private TaxAffectation taxAffectation;
  @Column(name = "tax_rate", nullable = false, precision = 5, scale = 2) private BigDecimal taxRate;
  @Column(name = "gross_amount", nullable = false, precision = 20, scale = 2) private BigDecimal grossAmount;
  @Column(name = "taxable_amount", nullable = false, precision = 20, scale = 2) private BigDecimal taxableAmount;
  @Column(name = "tax_amount", nullable = false, precision = 20, scale = 2) private BigDecimal taxAmount;
  @Column(name = "line_total", nullable = false, precision = 20, scale = 2) private BigDecimal lineTotal;
}
