package com.invoiceautomationservice.domain.model;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
public record InvoiceDraft(UUID id, InvoiceDraftStatus status, Instant createdAt) {
    public InvoiceDraft {
        Objects.requireNonNull(id, "id is required");
        Objects.requireNonNull(status, "status is required");
        Objects.requireNonNull(createdAt, "createdAt is required");
    }
    public static InvoiceDraft create(UUID id, Instant createdAt) {
        return new InvoiceDraft(id, InvoiceDraftStatus.DRAFT, createdAt);
    }
}
