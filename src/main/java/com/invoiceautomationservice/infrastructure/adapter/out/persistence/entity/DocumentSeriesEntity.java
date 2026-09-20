package com.invoiceautomationservice.infrastructure.adapter.out.persistence.entity;

import com.invoiceautomationservice.domain.model.InvoiceDocumentType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "document_series")
public class DocumentSeriesEntity {
  @Id
  private String id;
  @Column(name = "company_id", nullable = false)
  private String companyId;
  @Enumerated(EnumType.STRING)
  @Column(name = "document_type", nullable = false)
  private InvoiceDocumentType documentType;
  @Column(nullable = false, length = 4)
  private String series;
  @Column(name = "current_correlative", nullable = false)
  private Long currentCorrelative;
  @Column(nullable = false)
  private Boolean active;
  @Version
  @Column(nullable = false)
  private Long version;
}
