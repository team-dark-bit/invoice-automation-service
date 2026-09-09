package com.invoiceautomationservice.infrastructure.outbound.persistence;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
@Entity @Table(name = "invoice_draft")
class InvoiceDraftJpaEntity {
    @Id private UUID id;
    @Column(nullable = false) private String status;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    protected InvoiceDraftJpaEntity() {}
    InvoiceDraftJpaEntity(UUID id, String status, Instant createdAt) { this.id=id; this.status=status; this.createdAt=createdAt; }
    UUID getId() { return id; } String getStatus() { return status; } Instant getCreatedAt() { return createdAt; }
}
