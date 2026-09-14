package com.invoiceautomationservice.infrastructure.adapter.out.persistence.mapper.security;

import com.darkbit.security.domain.model.RoleModel;
import com.darkbit.security.domain.model.UserModel;
import com.invoiceautomationservice.infrastructure.adapter.out.persistence.entity.PermissionEntity;
import com.invoiceautomationservice.infrastructure.adapter.out.persistence.entity.RoleEntity;
import com.invoiceautomationservice.infrastructure.adapter.out.persistence.entity.UserEntity;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserEntitySecurityMapper {

  UserModel toModel(UserEntity entity);

  @Mapping(target = "roles", ignore = true)
  UserEntity toEntity(UserModel model);

  default Set<RoleModel> map(Set<RoleEntity> roles) {
    if (roles == null) {
      return Collections.emptySet();
    }
    return roles.stream()
        .map(this::mapRole)
        .collect(Collectors.toSet());
  }

  default RoleModel mapRole(RoleEntity role) {
    if (role == null) {
      return null;
    }
    return new RoleModel(role.getId(), role.getName(), mapPermissionNames(role.getPermissions()));
  }

  default Set<String> mapPermissionNames(Set<PermissionEntity> permissions) {
    if (permissions == null) {
      return Collections.emptySet();
    }
    return permissions.stream().map(PermissionEntity::getName).collect(Collectors.toSet());
  }
}

