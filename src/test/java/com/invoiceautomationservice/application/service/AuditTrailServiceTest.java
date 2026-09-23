package com.invoiceautomationservice.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.invoiceautomationservice.application.model.PageQuery;
import com.invoiceautomationservice.application.model.PageResult;
import com.invoiceautomationservice.application.port.out.AuditEventRepository;
import com.invoiceautomationservice.application.port.out.CurrentUserProvider;
import com.invoiceautomationservice.domain.model.AuditAction;
import com.invoiceautomationservice.domain.model.AuditEvent;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class AuditTrailServiceTest {
  private AuditEventRepository repository;
  private CurrentUserProvider currentUserProvider;
  private CompanyAccessService companyAccessService;
  private AuditTrailService service;

  @BeforeEach
  void setUp() {
    repository = mock(AuditEventRepository.class);
    currentUserProvider = mock(CurrentUserProvider.class);
    companyAccessService = mock(CompanyAccessService.class);
    service = new AuditTrailService(repository, currentUserProvider, companyAccessService,
        Clock.fixed(Instant.parse("2026-09-23T15:00:00Z"), ZoneOffset.UTC));
  }

  @Test
  void recordsAuthenticatedActorAndUtcTimestamp() {
    when(currentUserProvider.username()).thenReturn("owner");
    when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    service.record("company-1", AuditAction.DRAFT_APPROVED, "INVOICE_DRAFT",
        "draft-1", "SUCCESS", "Draft approved");

    ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
    verify(repository).save(captor.capture());
    assertThat(captor.getValue().username()).isEqualTo("owner");
    assertThat(captor.getValue().occurredAt()).isEqualTo("2026-09-23T15:00:00Z");
  }

  @Test
  void searchesOnlyResolvedCompanyContext() {
    when(companyAccessService.resolveCompanyId("company-1")).thenReturn("company-1");
    AuditEvent event = new AuditEvent(java.util.UUID.randomUUID(), "company-1", "owner",
        AuditAction.DRAFT_CREATED, "INVOICE_DRAFT", "draft-1", "SUCCESS", null,
        Instant.parse("2026-09-23T15:00:00Z"));
    when(repository.search("company-1", AuditAction.DRAFT_CREATED, null, null,
        null, null, new PageQuery(0, 20)))
        .thenReturn(new PageResult<>(List.of(event), 0, 20, 1, 1));

    var result = service.search("company-1", AuditAction.DRAFT_CREATED, null, null,
        null, null, 0, 20);

    assertThat(result.content()).hasSize(1);
    assertThat(result.content().getFirst().username()).isEqualTo("owner");
  }

  @Test
  void rejectsInvertedDateRange() {
    when(companyAccessService.resolveCompanyId("company-1")).thenReturn("company-1");
    assertThatThrownBy(() -> service.search("company-1", null, null, null,
        Instant.parse("2026-09-24T00:00:00Z"), Instant.parse("2026-09-23T00:00:00Z"), 0, 20))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
