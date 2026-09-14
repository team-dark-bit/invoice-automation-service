package com.invoiceautomationservice.infrastructure.adapter.out.persistence.repository;

import com.invoiceautomationservice.infrastructure.adapter.out.persistence.entity.InvoiceDraftEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaInvoiceDraftRepository extends JpaRepository<InvoiceDraftEntity, UUID> {
}
