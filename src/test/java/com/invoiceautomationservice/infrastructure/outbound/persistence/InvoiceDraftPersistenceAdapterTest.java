package com.invoiceautomationservice.infrastructure.outbound.persistence;
import com.invoiceautomationservice.domain.model.InvoiceDraft;
import com.invoiceautomationservice.domain.model.InvoiceDraftStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import java.time.Instant;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
@Testcontainers(disabledWithoutDocker = true)
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@Import(InvoiceDraftPersistenceAdapter.class)
class InvoiceDraftPersistenceAdapterTest {
    @Container static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");
    @DynamicPropertySource static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }
    @Autowired InvoiceDraftPersistenceAdapter adapter;
    @Test void persistsDraft() {
        var draft = new InvoiceDraft(UUID.randomUUID(), InvoiceDraftStatus.DRAFT, Instant.parse("2026-09-01T10:00:00Z"));
        assertThat(adapter.save(draft)).isEqualTo(draft);
    }
}
