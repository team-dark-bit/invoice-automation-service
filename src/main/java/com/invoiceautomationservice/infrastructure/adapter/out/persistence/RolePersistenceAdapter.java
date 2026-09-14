package com.invoiceautomationservice.infrastructure.adapter.out.persistence;

import com.darkbit.security.application.port.RolePort;
import com.darkbit.security.domain.model.RoleModel;
import com.invoiceautomationservice.infrastructure.adapter.out.persistence.entity.PermissionEntity;
import com.invoiceautomationservice.infrastructure.adapter.out.persistence.entity.RoleEntity;
import com.invoiceautomationservice.infrastructure.adapter.out.persistence.repository.JpaRoleRepository;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class RolePersistenceAdapter implements RolePort {

  private final JpaRoleRepository roleRepository;

  public RolePersistenceAdapter(JpaRoleRepository roleRepository) {
    this.roleRepository = roleRepository;
  }

  @Override
  public Optional<RoleModel> findById(String id) {
    return roleRepository.findById(id)
            .map(r -> new RoleModel(
                    r.getId(),
                    r.getName(),
                    r.getPermissions().stream()
                            .map(PermissionEntity::getName)
                            .collect(Collectors.toSet())
            ));
  }
}