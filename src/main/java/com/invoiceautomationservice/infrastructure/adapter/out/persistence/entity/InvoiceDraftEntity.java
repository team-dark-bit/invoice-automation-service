package com.invoiceautomationservice.infrastructure.adapter.out.persistence.entity;

import com.invoiceautomationservice.domain.model.InvoiceDraftStatus;
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
@Entity(name = "InvoiceDraft")
@Table(name = "invoice_draft")
public class InvoiceDraftEntity {
  @Id
  private UUID id;
  @Column(name = "company_id", nullable = false, length = 36)
  private String companyId;
  @Column(name = "customer_id", nullable = false, length = 36)
  private String customerId;
  @Enumerated(EnumType.STRING)
  @Column(name = "document_type", nullable = false)
  private InvoiceDocumentType documentType;
  @Enumerated(EnumType.STRING)
  @Column(name = "recipient_document_type", nullable = false)
  private IdentityDocumentType recipientDocumentType;
  @Column(name = "recipient_document_number", nullable = false, length = 32)
  private String recipientDocumentNumber;
  @Column(nullable = false, length = 3)
  private String currency;
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private InvoiceDraftStatus status;
  @Column(nullable = false, precision = 20, scale = 6)
  private BigDecimal subtotal;
  @Column(name = "discount_total", nullable = false, precision = 20, scale = 2)
  private BigDecimal discountTotal;
  @Column(name = "taxable_total", nullable = false, precision = 20, scale = 2)
  private BigDecimal taxableTotal;
  @Column(name = "tax_total", nullable = false, precision = 20, scale = 2)
  private BigDecimal taxTotal;
  @Column(nullable = false, precision = 20, scale = 6)
  private BigDecimal total;
  @Column(name = "created_at", nullable = false)
  private Instant createdAt;
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;
  @Column(name = "provider_reference", length = 255)
  private String providerReference;
  @Column(name = "issued_at")
  private Instant issuedAt;
}
