package com.invoiceautomationservice.infrastructure.inbound.rest;
import com.invoiceautomationservice.domain.model.InvoiceDraft;
import com.invoiceautomationservice.domain.model.InvoiceDraftStatus;
import java.time.Instant;
import java.util.UUID;
public record InvoiceDraftResponse(UUID id, InvoiceDraftStatus status, Instant createdAt) {
    static InvoiceDraftResponse from(InvoiceDraft draft) { return new InvoiceDraftResponse(draft.id(), draft.status(), draft.createdAt()); }
}
