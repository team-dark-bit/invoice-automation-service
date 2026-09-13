package com.invoiceautomationservice.infrastructure.adapter.out.persistence.repository;

import com.invoiceautomationservice.infrastructure.adapter.out.persistence.entity.CompanyDao;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaCompanyRepository extends JpaRepository<CompanyDao, String> {
  List<CompanyDao> findAllByActiveTrue();
}
