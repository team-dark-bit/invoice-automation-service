package com.invoiceautomationservice.infrastructure.adapter.out.persistence.repository;

import com.invoiceautomationservice.infrastructure.adapter.out.persistence.entity.CustomerEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaCustomerRepository extends JpaRepository<CustomerEntity, String>,
    org.springframework.data.jpa.repository.JpaSpecificationExecutor<CustomerEntity> {
    Optional<CustomerEntity> findByIdAndCompanyId(String id, String companyId);
    List<CustomerEntity> findAllByCompanyIdAndActiveTrue(String companyId);
    Optional<CustomerEntity> findByCompanyIdAndDocumentTypeAndDocumentNumber(
        String companyId, String documentType, String documentNumber);
}

