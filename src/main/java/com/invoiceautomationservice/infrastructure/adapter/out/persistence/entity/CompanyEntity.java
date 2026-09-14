package com.invoiceautomationservice.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity(name = "Company")
@Table(name = "companies")
public class CompanyEntity {

  @Id
  private String id;

  @Column(name = "legal_name", nullable = false)
  private String legalName;

  @Column(name = "trade_name")
  private String tradeName;

  @Column(name = "tax_id", nullable = false, unique = true)
  private String taxId;

  private String address;

  @Column(nullable = false)
  private Boolean active;
}
