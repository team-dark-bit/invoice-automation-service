package com.invoiceautomationservice.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "users")
public class UserEntity {

  @Id
  private String id;
  private String fullName;
  private String username;
  private String email;
  private String password;
  private boolean enabled;
  private LocalDateTime createdAt;
  private LocalDateTime lastLogin;

  // campos para un futuro control de seguridad, aunque por ahora no los usaremos:
  // private boolean accountNonExpired = true;
  // private boolean accountNonLocked = true;
  // private boolean credentialsNonExpired = true;

  @ManyToMany(fetch = FetchType.EAGER)
  @JoinTable(
          name = "user_roles",
          joinColumns = @JoinColumn(name = "user_id"),
          inverseJoinColumns = @JoinColumn(name = "role_id")
  )
  private Set<RoleEntity> roles = new HashSet<>();

}
