package com.invoiceautomationservice.infrastructure.adapter.out.persistence.repository;

import com.invoiceautomationservice.infrastructure.adapter.out.persistence.entity.InvoiceDraftEntity;
import java.util.UUID;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaInvoiceDraftRepository extends JpaRepository<InvoiceDraftEntity, UUID>,
    org.springframework.data.jpa.repository.JpaSpecificationExecutor<InvoiceDraftEntity> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select draft from InvoiceDraft draft where draft.id = :id")
  java.util.Optional<InvoiceDraftEntity> findByIdForUpdate(@Param("id") UUID id);
}
