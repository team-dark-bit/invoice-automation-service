package com.invoiceautomationservice.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import com.invoiceautomationservice.domain.model.CompanyRole;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

@Getter
@Setter
@NoArgsConstructor
@Entity
@IdClass(UserCompanyId.class)
@Table(name = "user_companies")
public class UserCompanyEntity {

  @Id
  @Column(name = "user_id", nullable = false)
  private String userId;

  @Id
  @Column(name = "company_id", nullable = false)
  private String companyId;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private CompanyRole role;

  @Column(nullable = false)
  private boolean active;

  public UserCompanyEntity(String userId, String companyId, Instant createdAt, CompanyRole role) {
    this.userId = userId;
    this.companyId = companyId;
    this.createdAt = createdAt;
    this.role = role;
    this.active = true;
  }
}
