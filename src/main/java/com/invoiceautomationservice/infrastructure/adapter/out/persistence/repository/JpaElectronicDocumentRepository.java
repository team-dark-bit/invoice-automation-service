package com.invoiceautomationservice.infrastructure.adapter.out.persistence.repository;

import com.invoiceautomationservice.infrastructure.adapter.out.persistence.entity.ElectronicDocumentEntity;
import java.util.UUID;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaElectronicDocumentRepository
    extends JpaRepository<ElectronicDocumentEntity, UUID>,
    org.springframework.data.jpa.repository.JpaSpecificationExecutor<ElectronicDocumentEntity> {
  Optional<ElectronicDocumentEntity> findByDraftId(UUID draftId);
  Optional<ElectronicDocumentEntity> findByRelatedDocumentIdAndDocumentTypeAndNoteReasonCode(
      UUID relatedDocumentId,
      com.invoiceautomationservice.domain.model.InvoiceDocumentType documentType,
      String noteReasonCode);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select document from ElectronicDocumentEntity document where document.id = :id")
  Optional<ElectronicDocumentEntity> findByIdForUpdate(@Param("id") UUID id);
}
