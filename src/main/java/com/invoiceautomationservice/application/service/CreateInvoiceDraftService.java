package com.invoiceautomationservice.application.service;
import com.invoiceautomationservice.application.port.in.CreateInvoiceDraftUseCase;
import com.invoiceautomationservice.application.port.out.InvoiceDraftRepository;
import com.invoiceautomationservice.domain.model.InvoiceDraft;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
public final class CreateInvoiceDraftService implements CreateInvoiceDraftUseCase {
    private final InvoiceDraftRepository repository;
    private final Clock clock;
    public CreateInvoiceDraftService(InvoiceDraftRepository repository, Clock clock) {
        this.repository = repository; this.clock = clock;
    }
    @Override public InvoiceDraft create() {
        return repository.save(InvoiceDraft.create(UUID.randomUUID(), Instant.now(clock)));
    }
}
