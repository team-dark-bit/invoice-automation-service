package com.invoiceautomationservice.infrastructure.adapter.out.persistence.repository;

import com.invoiceautomationservice.infrastructure.adapter.out.persistence.entity.InvoiceItemEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaInvoiceItemRepository extends JpaRepository<InvoiceItemEntity, UUID> {
  List<InvoiceItemEntity> findAllByInvoiceDraftIdOrderByPosition(UUID invoiceDraftId);
}
