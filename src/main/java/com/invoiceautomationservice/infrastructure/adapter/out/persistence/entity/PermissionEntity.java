package com.invoiceautomationservice.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "permissions")
public class PermissionEntity {

  @Id
  private String id;

  private String name; // USER_CREATE, USER_DELETE, POST_CREATE, etc.

  private String description;

  @Column(nullable = false)
  private String category; // USER, POST, COMMENT, REPORT (para agrupar en UI)
}
