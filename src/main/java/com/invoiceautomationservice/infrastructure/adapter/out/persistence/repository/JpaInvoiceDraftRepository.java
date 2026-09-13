package com.invoiceautomationservice.infrastructure.adapter.out.persistence.repository;

import com.invoiceautomationservice.infrastructure.adapter.out.persistence.entity.InvoiceDraftDao;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaInvoiceDraftRepository extends JpaRepository<InvoiceDraftDao, UUID> {
}
