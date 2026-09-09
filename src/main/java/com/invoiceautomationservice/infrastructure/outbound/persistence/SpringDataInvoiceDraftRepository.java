package com.invoiceautomationservice.infrastructure.outbound.persistence;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
interface SpringDataInvoiceDraftRepository extends JpaRepository<InvoiceDraftJpaEntity, UUID> {}
