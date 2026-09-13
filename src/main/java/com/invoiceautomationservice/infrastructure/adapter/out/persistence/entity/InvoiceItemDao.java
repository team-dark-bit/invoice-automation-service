package com.invoiceautomationservice.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity(name = "InvoiceItem")
@Table(name = "invoice_items")
public class InvoiceItemDao {
  @Id
  private UUID id;
  @Column(name = "invoice_draft_id", nullable = false)
  private UUID invoiceDraftId;
  @Column(nullable = false)
  private Integer position;
  @Column(nullable = false, length = 500)
  private String description;
  @Column(nullable = false, precision = 16, scale = 4)
  private BigDecimal quantity;
  @Column(name = "unit_price", nullable = false, precision = 16, scale = 2)
  private BigDecimal unitPrice;
  @Column(name = "line_total", nullable = false, precision = 20, scale = 6)
  private BigDecimal lineTotal;
}
