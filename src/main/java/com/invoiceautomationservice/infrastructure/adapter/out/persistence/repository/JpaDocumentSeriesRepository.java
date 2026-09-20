package com.invoiceautomationservice.infrastructure.adapter.out.persistence.repository;

import com.invoiceautomationservice.domain.model.InvoiceDocumentType;
import com.invoiceautomationservice.infrastructure.adapter.out.persistence.entity.DocumentSeriesEntity;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface JpaDocumentSeriesRepository extends JpaRepository<DocumentSeriesEntity, String> {
  List<DocumentSeriesEntity> findAllByCompanyIdOrderBySeries(String companyId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<DocumentSeriesEntity> findFirstByCompanyIdAndDocumentTypeAndActiveTrueOrderBySeries(
      String companyId, InvoiceDocumentType documentType);
}
