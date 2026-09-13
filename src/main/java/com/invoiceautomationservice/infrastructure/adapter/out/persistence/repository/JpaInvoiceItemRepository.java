package com.invoiceautomationservice.infrastructure.adapter.out.persistence.repository;

import com.invoiceautomationservice.infrastructure.adapter.out.persistence.entity.InvoiceItemDao;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaInvoiceItemRepository extends JpaRepository<InvoiceItemDao, UUID> {
  List<InvoiceItemDao> findAllByInvoiceDraftIdOrderByPosition(UUID invoiceDraftId);
}
