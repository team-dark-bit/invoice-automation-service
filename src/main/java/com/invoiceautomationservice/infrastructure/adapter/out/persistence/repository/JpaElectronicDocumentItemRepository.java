package com.invoiceautomationservice.infrastructure.adapter.out.persistence.repository;

import com.invoiceautomationservice.infrastructure.adapter.out.persistence.entity.ElectronicDocumentItemEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaElectronicDocumentItemRepository
    extends JpaRepository<ElectronicDocumentItemEntity, UUID> {
  List<ElectronicDocumentItemEntity> findAllByDocumentIdOrderByPosition(UUID documentId);
}
