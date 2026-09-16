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

  public UserCompanyEntity(String userId, String companyId, Instant createdAt) {
    this.userId = userId;
    this.companyId = companyId;
    this.createdAt = createdAt;
  }
}
