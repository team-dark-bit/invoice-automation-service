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
@Table(name = "conversation_context_items")
public class ConversationContextItemEntity {
  @Id private UUID id;
  @Column(name = "conversation_id", nullable = false) private UUID conversationId;
  @Column(nullable = false) private int position;
  @Column(nullable = false, length = 500) private String description;
  @Enumerated(EnumType.STRING) @Column(name = "unit_code", nullable = false) private UnitCode unitCode;
  @Column(nullable = false, precision = 16, scale = 4) private BigDecimal quantity;
  @Column(name = "unit_price", nullable = false, precision = 16, scale = 2) private BigDecimal unitPrice;
  @Column(nullable = false, precision = 16, scale = 2) private BigDecimal discount;
  @Enumerated(EnumType.STRING) @Column(name = "tax_affectation", nullable = false)
  private TaxAffectation taxAffectation;
}
