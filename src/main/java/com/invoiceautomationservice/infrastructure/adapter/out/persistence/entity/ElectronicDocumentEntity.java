package com.invoiceautomationservice.infrastructure.adapter.out.persistence.entity;

import com.invoiceautomationservice.domain.model.IdentityDocumentType;
import com.invoiceautomationservice.domain.model.InvoiceDocumentType;
import com.invoiceautomationservice.domain.model.ElectronicDocumentStatus;
import com.invoiceautomationservice.domain.model.TaxpayerType;
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
  @Column(name = "issuer_tax_id", nullable = false) private String issuerTaxId;
  @Column(name = "issuer_legal_name", nullable = false) private String issuerLegalName;
  @Column(name = "issuer_trade_name") private String issuerTradeName;
  @Enumerated(EnumType.STRING) @Column(name = "issuer_taxpayer_type", nullable = false)
  private TaxpayerType issuerTaxpayerType;
  @Column(name = "issuer_fiscal_address", nullable = false) private String issuerFiscalAddress;
  @Column(name = "issuer_ubigeo", nullable = false, length = 6) private String issuerUbigeo;
  @Column(name = "issuer_department") private String issuerDepartment;
  @Column(name = "issuer_province") private String issuerProvince;
  @Column(name = "issuer_district") private String issuerDistrict;
  @Column(name = "issuer_country_code", nullable = false, length = 2) private String issuerCountryCode;
  @Column(name = "recipient_name", nullable = false) private String recipientName;
  @Column(name = "recipient_address") private String recipientAddress;
  @Column(name = "recipient_email") private String recipientEmail;
  @Enumerated(EnumType.STRING) @Column(name = "document_type", nullable = false)
  private InvoiceDocumentType documentType;
  @Column(nullable = false, length = 4) private String series;
  @Column(nullable = false) private Long correlative;
  @Column(name = "full_number", nullable = false) private String fullNumber;
  @Enumerated(EnumType.STRING) @Column(name = "recipient_document_type", nullable = false)
  private IdentityDocumentType recipientDocumentType;
  @Column(name = "recipient_document_number", nullable = false) private String recipientDocumentNumber;
  @Column(nullable = false, length = 3) private String currency;
  @Column(name = "emission_at", nullable = false) private Instant emissionAt;
  @Column(nullable = false, precision = 20, scale = 2) private BigDecimal subtotal;
  @Column(name = "discount_total", nullable = false, precision = 20, scale = 2)
  private BigDecimal discountTotal;
  @Column(name = "taxable_total", nullable = false, precision = 20, scale = 2)
  private BigDecimal taxableTotal;
  @Column(name = "tax_total", nullable = false, precision = 20, scale = 2)
  private BigDecimal taxTotal;
  @Column(nullable = false, precision = 20, scale = 2) private BigDecimal total;
  @Enumerated(EnumType.STRING) @Column(nullable = false)
  private ElectronicDocumentStatus status;
  @Column(name = "provider_reference") private String providerReference;
  @Column(name = "submitted_at") private Instant submittedAt;
  @Column(name = "responded_at") private Instant respondedAt;
  @Column(name = "provider_response_code", length = 100) private String providerResponseCode;
  @Column(name = "provider_response_message", length = 1000) private String providerResponseMessage;
}
