package com.invoiceautomationservice.application.service;
import com.invoiceautomationservice.application.port.out.InvoiceDraftRepository;
import com.invoiceautomationservice.domain.model.InvoiceDraftStatus;
import org.junit.jupiter.api.Test;
import java.time.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
class CreateInvoiceDraftServiceTest {
    @Test void createsAndPersistsDraft() {
        var repository = mock(InvoiceDraftRepository.class);
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var instant = Instant.parse("2026-09-01T10:00:00Z");
        var service = new CreateInvoiceDraftService(repository, Clock.fixed(instant, ZoneOffset.UTC));
        var result = service.create();
        assertThat(result.id()).isNotNull();
        assertThat(result.status()).isEqualTo(InvoiceDraftStatus.DRAFT);
        assertThat(result.createdAt()).isEqualTo(instant);
        verify(repository).save(result);
    }
}
