package com.invoiceautomationservice.infrastructure.adapter.out.persistence;

import com.invoiceautomationservice.application.port.out.UserCompanyRepository;
import com.invoiceautomationservice.infrastructure.adapter.out.persistence.entity.UserCompanyEntity;
import com.invoiceautomationservice.infrastructure.adapter.out.persistence.repository.JpaUserCompanyRepository;
import com.invoiceautomationservice.infrastructure.adapter.out.persistence.repository.JpaUserRepository;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import com.invoiceautomationservice.domain.model.CompanyRole;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class UserCompanyPersistenceAdapter implements UserCompanyRepository {

  private final JpaUserRepository userRepository;
  private final JpaUserCompanyRepository userCompanyRepository;

  @Override
  public void associate(String username, String companyId) {
    String userId = findUserId(username);
    if (!userCompanyRepository.existsByUserIdAndCompanyId(userId, companyId)) {
      userCompanyRepository.save(new UserCompanyEntity(
          userId, companyId, Instant.now(), CompanyRole.OWNER));
    }
  }

  @Override
  public boolean hasAccess(String username, String companyId) {
    return userCompanyRepository.existsByUserIdAndCompanyIdAndActiveTrue(
        findUserId(username), companyId);
  }

  @Override
  public List<String> findCompanyIds(String username) {
    return userCompanyRepository.findAllByUserIdAndActiveTrueOrderByCreatedAtAsc(
        findUserId(username)).stream()
        .map(UserCompanyEntity::getCompanyId)
        .toList();
  }

  @Override
  public Optional<CompanyRole> findRole(String username, String companyId) {
    return userCompanyRepository.findByUserIdAndCompanyId(findUserId(username), companyId)
        .filter(UserCompanyEntity::isActive)
        .map(UserCompanyEntity::getRole);
  }

  private String findUserId(String username) {
    return userRepository.findByUsername(username)
        .orElseThrow(() -> new IllegalStateException("Authenticated user no longer exists"))
        .getId();
  }
}
