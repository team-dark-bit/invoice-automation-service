package com.invoiceautomationservice.infrastructure.adapter.out.persistence.repository;

import com.invoiceautomationservice.infrastructure.adapter.out.persistence.entity.UserCompanyEntity;
import com.invoiceautomationservice.infrastructure.adapter.out.persistence.entity.UserCompanyId;
import java.util.List;
import com.invoiceautomationservice.domain.model.CompanyRole;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaUserCompanyRepository extends JpaRepository<UserCompanyEntity, UserCompanyId> {
  boolean existsByUserIdAndCompanyId(String userId, String companyId);
  boolean existsByUserIdAndCompanyIdAndActiveTrue(String userId, String companyId);
  List<UserCompanyEntity> findAllByUserIdAndActiveTrueOrderByCreatedAtAsc(String userId);
  java.util.Optional<UserCompanyEntity> findByUserIdAndCompanyId(String userId, String companyId);
  List<UserCompanyEntity> findAllByCompanyIdOrderByCreatedAtAsc(String companyId);
  long countByCompanyIdAndRoleAndActiveTrue(String companyId, CompanyRole role);
}
