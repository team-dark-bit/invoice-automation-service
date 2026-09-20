package com.invoiceautomationservice.infrastructure.adapter.out.persistence.entity;

import com.invoiceautomationservice.domain.model.TaxpayerType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "issuer_tax_profiles")
public class IssuerTaxProfileEntity {
  @Id
  @Column(name = "company_id")
  private String companyId;
  @Enumerated(EnumType.STRING)
  @Column(name = "taxpayer_type", nullable = false)
  private TaxpayerType taxpayerType;
  @Column(name = "fiscal_address", nullable = false)
  private String fiscalAddress;
  @Column(nullable = false, length = 6)
  private String ubigeo;
  @Column(nullable = false)
  private String department;
  @Column(nullable = false)
  private String province;
  @Column(nullable = false)
  private String district;
  @Column(name = "country_code", nullable = false, length = 2)
  private String countryCode;
  @Column(name = "created_at", nullable = false)
  private Instant createdAt;
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;
}
