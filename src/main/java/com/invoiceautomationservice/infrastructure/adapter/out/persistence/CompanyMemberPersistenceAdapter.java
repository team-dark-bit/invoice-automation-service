package com.invoiceautomationservice.infrastructure.adapter.out.persistence;

import com.invoiceautomationservice.application.port.out.CompanyMemberRepository;
import com.invoiceautomationservice.domain.model.CompanyMember;
import com.invoiceautomationservice.domain.model.CompanyRole;
import com.invoiceautomationservice.infrastructure.adapter.out.persistence.entity.UserCompanyEntity;
import com.invoiceautomationservice.infrastructure.adapter.out.persistence.entity.UserCompanyId;
import com.invoiceautomationservice.infrastructure.adapter.out.persistence.entity.UserEntity;
import com.invoiceautomationservice.infrastructure.adapter.out.persistence.repository.JpaRoleRepository;
import com.invoiceautomationservice.infrastructure.adapter.out.persistence.repository.JpaUserCompanyRepository;
import com.invoiceautomationservice.infrastructure.adapter.out.persistence.repository.JpaUserRepository;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import com.invoiceautomationservice.infrastructure.config.exception.ApplicationException;
import static com.invoiceautomationservice.infrastructure.config.exception.RuntimeErrors.*;

@Repository
@RequiredArgsConstructor
public class CompanyMemberPersistenceAdapter implements CompanyMemberRepository {
  private final JpaUserRepository userRepository;
  private final JpaUserCompanyRepository membershipRepository;
  private final JpaRoleRepository roleRepository;

  @Override
  public boolean userExists(String username, String email) {
    return userRepository.existsByUsername(username) || userRepository.existsByEmail(email);
  }

  @Override
  public CompanyMember create(String companyId, String fullName, String username, String email,
      String passwordHash, CompanyRole role, Instant joinedAt) {
    UserEntity user = new UserEntity();
    user.setId(UUID.randomUUID().toString());
    user.setFullName(fullName);
    user.setUsername(username);
    user.setEmail(email);
    user.setPassword(passwordHash);
    user.setEnabled(true);
    user.setCreatedAt(LocalDateTime.ofInstant(joinedAt, ZoneOffset.UTC));
    user.getRoles().add(roleRepository.findByName("ROLE_USER")
        .orElseThrow(() -> new IllegalStateException("ROLE_USER seed is missing")));
    userRepository.save(user);
    UserCompanyEntity membership = membershipRepository.save(
        new UserCompanyEntity(user.getId(), companyId, joinedAt, role));
    return toDomain(user, membership);
  }

  @Override
  public CompanyMember associateExisting(
      String companyId, String username, CompanyRole role, Instant joinedAt) {
    UserEntity user = userRepository.findByUsername(username)
        .orElseThrow(() -> new ApplicationException(USER_NOT_FOUND, username));
    if (membershipRepository.existsByUserIdAndCompanyId(user.getId(), companyId)) {
      throw new ApplicationException(COMPANY_MEMBER_ALREADY_EXISTS, companyId);
    }
    UserCompanyEntity membership = membershipRepository.save(
        new UserCompanyEntity(user.getId(), companyId, joinedAt, role));
    return toDomain(user, membership);
  }

  @Override
  public List<CompanyMember> findAll(String companyId) {
    return membershipRepository.findAllByCompanyIdOrderByCreatedAtAsc(companyId).stream()
        .map(membership -> toDomain(findUser(membership.getUserId()), membership)).toList();
  }

  @Override
  public CompanyMember findByUserId(String companyId, String userId) {
    UserCompanyEntity membership = membershipRepository.findByUserIdAndCompanyId(userId, companyId)
        .orElseThrow(() -> new ApplicationException(COMPANY_MEMBER_NOT_FOUND, userId, companyId));
    return toDomain(findUser(userId), membership);
  }

  @Override
  public CompanyMember update(String companyId, String userId, CompanyRole role, Boolean enabled) {
    UserCompanyEntity membership = membershipRepository.findByUserIdAndCompanyId(userId, companyId)
        .orElseThrow(() -> new ApplicationException(COMPANY_MEMBER_NOT_FOUND, userId, companyId));
    if (role != null) membership.setRole(role);
    UserEntity user = findUser(userId);
    if (enabled != null) membership.setActive(enabled);
    return toDomain(user, membershipRepository.save(membership));
  }

  @Override
  public void remove(String companyId, String userId) {
    membershipRepository.deleteById(new UserCompanyId(userId, companyId));
  }

  @Override
  public long countOwners(String companyId) {
    return membershipRepository.countByCompanyIdAndRoleAndActiveTrue(companyId, CompanyRole.OWNER);
  }

  private UserEntity findUser(String userId) {
    return userRepository.findById(userId)
        .orElseThrow(() -> new IllegalArgumentException("user does not exist"));
  }

  private CompanyMember toDomain(UserEntity user, UserCompanyEntity membership) {
    return new CompanyMember(user.getId(), membership.getCompanyId(), user.getFullName(),
        user.getUsername(), user.getEmail(), membership.isActive(), membership.getRole(),
        membership.getRole().permissions(), membership.getCreatedAt());
  }
}
