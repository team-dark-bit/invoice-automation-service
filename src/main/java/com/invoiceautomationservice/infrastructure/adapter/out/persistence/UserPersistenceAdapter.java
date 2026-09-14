package com.invoiceautomationservice.infrastructure.adapter.out.persistence;

import com.darkbit.security.application.port.UserPort;
import com.darkbit.security.domain.model.RoleModel;
import com.darkbit.security.domain.model.UserModel;
import com.invoiceautomationservice.infrastructure.adapter.out.persistence.entity.RoleEntity;
import com.invoiceautomationservice.infrastructure.adapter.out.persistence.entity.UserEntity;
import com.invoiceautomationservice.infrastructure.adapter.out.persistence.mapper.security.UserEntitySecurityMapper;
import com.invoiceautomationservice.infrastructure.adapter.out.persistence.repository.JpaRoleRepository;
import com.invoiceautomationservice.infrastructure.adapter.out.persistence.repository.JpaUserRepository;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class UserPersistenceAdapter implements UserPort {

  private final JpaUserRepository userRepository;
  private final JpaRoleRepository roleRepository;
  private final UserEntitySecurityMapper userMapper;

  public UserPersistenceAdapter(
      JpaUserRepository userRepository,
      JpaRoleRepository roleRepository,
      UserEntitySecurityMapper userMapper) {
    this.userRepository = userRepository;
    this.roleRepository = roleRepository;
    this.userMapper = userMapper;
  }

  @Override
  public boolean existsByUsername(String username) {
    return userRepository.existsByUsername(username);
  }

  @Override
  public boolean existsByEmail(String email) {
    return userRepository.existsByEmail(email);
  }

  @Override
  public Optional<UserModel> findByUsername(String username) {
    return userRepository.findByUsername(username).map(userMapper::toModel);
  }

  @Override
  public void save(UserModel model) {
    UserEntity entity = userMapper.toEntity(model);
    entity.setRoles(resolveRoles(model.getRoles()));
    userRepository.save(entity);
  }

  private Set<RoleEntity> resolveRoles(Set<RoleModel> roles) {
    Set<String> roleIds = roles.stream().map(RoleModel::getId).collect(Collectors.toSet());

    Map<String, RoleEntity> rolesById =
        roleRepository.findAllById(roleIds).stream()
            .collect(Collectors.toMap(RoleEntity::getId, role -> role));

    if (rolesById.size() != roleIds.size()) {
      String missingRoleIds =
          roleIds.stream().filter(id -> !rolesById.containsKey(id)).collect(Collectors.joining(", "));
      throw new IllegalArgumentException("Role not found: " + missingRoleIds);
    }

    return new HashSet<>(rolesById.values());
  }
}