package com.invoiceautomationservice.infrastructure.outbound.persistence;
import com.invoiceautomationservice.application.port.out.InvoiceDraftRepository;
import com.invoiceautomationservice.domain.model.InvoiceDraft;
import com.invoiceautomationservice.domain.model.InvoiceDraftStatus;
import org.springframework.stereotype.Repository;
@Repository
public class InvoiceDraftPersistenceAdapter implements InvoiceDraftRepository {
    private final SpringDataInvoiceDraftRepository repository;
    public InvoiceDraftPersistenceAdapter(SpringDataInvoiceDraftRepository repository) { this.repository = repository; }
    @Override public InvoiceDraft save(InvoiceDraft draft) {
        var saved = repository.save(new InvoiceDraftJpaEntity(draft.id(), draft.status().name(), draft.createdAt()));
        return new InvoiceDraft(saved.getId(), InvoiceDraftStatus.valueOf(saved.getStatus()), saved.getCreatedAt());
    }
}
