package com.invoiceautomationservice.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "roles")
public class RoleEntity {
  @Id
  private String id;

  private String name; // ROLE_USER, ROLE_ADMIN

  private String description;

  @ManyToMany(fetch = FetchType.EAGER)
  @JoinTable(
          name = "role_permissions",
          joinColumns = @JoinColumn(name = "role_id"),
          inverseJoinColumns = @JoinColumn(name = "permission_id")
  )
  private Set<PermissionEntity> permissions = new HashSet<>();
}
