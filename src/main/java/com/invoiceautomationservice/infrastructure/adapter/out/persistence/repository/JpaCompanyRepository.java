package com.invoiceautomationservice.infrastructure.adapter.out.persistence.repository;

import com.invoiceautomationservice.infrastructure.adapter.out.persistence.entity.CompanyEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaCompanyRepository extends JpaRepository<CompanyEntity, String>,
    org.springframework.data.jpa.repository.JpaSpecificationExecutor<CompanyEntity> {
  List<CompanyEntity> findAllByIdInAndActiveTrue(List<String> ids);
}
