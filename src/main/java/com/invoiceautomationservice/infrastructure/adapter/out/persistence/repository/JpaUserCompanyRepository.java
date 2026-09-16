package com.invoiceautomationservice.infrastructure.adapter.out.persistence.repository;

import com.invoiceautomationservice.infrastructure.adapter.out.persistence.entity.UserCompanyEntity;
import com.invoiceautomationservice.infrastructure.adapter.out.persistence.entity.UserCompanyId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaUserCompanyRepository extends JpaRepository<UserCompanyEntity, UserCompanyId> {
  boolean existsByUserIdAndCompanyId(String userId, String companyId);
  List<UserCompanyEntity> findAllByUserIdOrderByCreatedAtAsc(String userId);
}
