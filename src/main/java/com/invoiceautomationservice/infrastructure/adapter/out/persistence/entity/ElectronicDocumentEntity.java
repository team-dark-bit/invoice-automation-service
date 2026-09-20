package com.invoiceautomationservice.infrastructure.adapter.out.persistence.entity;

import com.invoiceautomationservice.domain.model.IdentityDocumentType;
import com.invoiceautomationservice.domain.model.InvoiceDocumentType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "electronic_documents")
public class ElectronicDocumentEntity {
  @Id private UUID id;
  @Column(name = "draft_id", nullable = false, unique = true) private UUID draftId;
  @Column(name = "company_id", nullable = false) private String companyId;
  @Column(name = "customer_id", nullable = false) private String customerId;
  @Enumerated(EnumType.STRING) @Column(name = "document_type", nullable = false)
  private InvoiceDocumentType documentType;
  @Column(nullable = false, length = 4) private String series;
  @Column(nullable = false) private Long correlative;
  @Column(name = "full_number", nullable = false) private String fullNumber;
  @Enumerated(EnumType.STRING) @Column(name = "recipient_document_type", nullable = false)
  private IdentityDocumentType recipientDocumentType;
  @Column(name = "recipient_document_number", nullable = false) private String recipientDocumentNumber;
  @Column(nullable = false, length = 3) private String currency;
  @Column(nullable = false, precision = 20, scale = 2) private BigDecimal subtotal;
  @Column(name = "discount_total", nullable = false, precision = 20, scale = 2)
  private BigDecimal discountTotal;
  @Column(name = "taxable_total", nullable = false, precision = 20, scale = 2)
  private BigDecimal taxableTotal;
  @Column(name = "tax_total", nullable = false, precision = 20, scale = 2)
  private BigDecimal taxTotal;
  @Column(nullable = false, precision = 20, scale = 2) private BigDecimal total;
  @Column(name = "provider_reference", nullable = false) private String providerReference;
  @Column(name = "issued_at", nullable = false) private Instant issuedAt;
}
