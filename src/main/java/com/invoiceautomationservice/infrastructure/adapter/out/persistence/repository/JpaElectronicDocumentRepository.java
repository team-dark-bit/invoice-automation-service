package com.invoiceautomationservice.infrastructure.adapter.out.persistence.repository;

import com.invoiceautomationservice.infrastructure.adapter.out.persistence.entity.ElectronicDocumentEntity;
import java.util.UUID;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaElectronicDocumentRepository
    extends JpaRepository<ElectronicDocumentEntity, UUID> {
  Optional<ElectronicDocumentEntity> findByDraftId(UUID draftId);
}
