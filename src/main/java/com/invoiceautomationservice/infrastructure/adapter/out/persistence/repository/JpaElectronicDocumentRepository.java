package com.invoiceautomationservice.infrastructure.adapter.out.persistence.repository;

import com.invoiceautomationservice.infrastructure.adapter.out.persistence.entity.ElectronicDocumentEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaElectronicDocumentRepository
    extends JpaRepository<ElectronicDocumentEntity, UUID> {
}
