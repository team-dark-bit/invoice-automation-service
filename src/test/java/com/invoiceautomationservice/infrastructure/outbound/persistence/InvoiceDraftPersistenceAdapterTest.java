package com.invoiceautomationservice.infrastructure.outbound.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.invoiceautomationservice.domain.model.InvoiceDraft;
import com.invoiceautomationservice.domain.model.InvoiceDraftStatus;
import com.invoiceautomationservice.domain.model.InvoiceItem;
import com.invoiceautomationservice.infrastructure.adapter.out.persistence.InvoiceDraftPersistenceAdapter;
import com.invoiceautomationservice.infrastructure.adapter.out.persistence.entity.InvoiceDraftDao;
import com.invoiceautomationservice.infrastructure.adapter.out.persistence.entity.InvoiceItemDao;
import com.invoiceautomationservice.infrastructure.adapter.out.persistence.mapper.domain.InvoiceDraftDaoDomainMapper;
import com.invoiceautomationservice.infrastructure.adapter.out.persistence.mapper.domain.InvoiceItemDaoDomainMapper;
import com.invoiceautomationservice.infrastructure.adapter.out.persistence.repository.JpaInvoiceDraftRepository;
import com.invoiceautomationservice.infrastructure.adapter.out.persistence.repository.JpaInvoiceItemRepository;
import com.invoiceautomationservice.infrastructure.config.exception.ApplicationException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InvoiceDraftPersistenceAdapterTest {

  private JpaInvoiceDraftRepository draftRepository;
  private JpaInvoiceItemRepository itemRepository;
  private InvoiceDraftDaoDomainMapper draftMapper;
  private InvoiceItemDaoDomainMapper itemMapper;
  private InvoiceDraftPersistenceAdapter adapter;

  @BeforeEach
  void setUp() {
    draftRepository = mock(JpaInvoiceDraftRepository.class);
    itemRepository = mock(JpaInvoiceItemRepository.class);
    draftMapper = mock(InvoiceDraftDaoDomainMapper.class);
    itemMapper = mock(InvoiceItemDaoDomainMapper.class);
    adapter = new InvoiceDraftPersistenceAdapter(draftRepository, itemRepository, draftMapper, itemMapper);
  }

  @Test
  void persistsDraftAndItemsAsOneAggregate() {
    InvoiceDraft draft = draft();
    InvoiceDraftDao draftDao = draftDao(draft);
    InvoiceItemDao itemDao = itemDao(draft.items().getFirst(), draft.id());
    when(draftMapper.toDao(draft)).thenReturn(draftDao);
    when(draftRepository.save(draftDao)).thenReturn(draftDao);
    when(itemMapper.toDao(draft.items().getFirst(), draft.id(), 0)).thenReturn(itemDao);
    when(itemRepository.saveAll(anyList())).thenReturn(List.of(itemDao));
    when(itemMapper.toDomain(itemDao)).thenReturn(draft.items().getFirst());

    InvoiceDraft saved = adapter.save(draft);

    assertThat(saved).isEqualTo(draft);
    verify(draftRepository).save(draftDao);
    verify(itemRepository).saveAll(List.of(itemDao));
  }

  @Test
  void loadsDraftWithItsItems() {
    InvoiceDraft draft = draft();
    InvoiceDraftDao draftDao = draftDao(draft);
    InvoiceItemDao itemDao = itemDao(draft.items().getFirst(), draft.id());
    when(draftRepository.findById(draft.id())).thenReturn(Optional.of(draftDao));
    when(itemRepository.findAllByInvoiceDraftIdOrderByPosition(draft.id())).thenReturn(List.of(itemDao));
    when(itemMapper.toDomain(itemDao)).thenReturn(draft.items().getFirst());

    assertThat(adapter.findById(draft.id())).isEqualTo(draft);
  }

  @Test
  void throwsWhenDraftDoesNotExistWithoutLoadingItems() {
    UUID id = UUID.fromString("4d773aa2-8ea7-4c26-935d-f47086d7c385");
    when(draftRepository.findById(id)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> adapter.findById(id))
            .isInstanceOf(ApplicationException.class)
            .hasMessage("The invoice draft with id: %s does not exist", id);
  }

  private InvoiceDraft draft() {
    Instant now = Instant.parse("2026-09-01T10:00:00Z");
    InvoiceItem item = InvoiceItem.create("Consulting", new BigDecimal("2"), new BigDecimal("150.25"));
    return new InvoiceDraft(
            UUID.fromString("4d773aa2-8ea7-4c26-935d-f47086d7c385"),
            "company-1", "customer-1", "PEN", InvoiceDraftStatus.DRAFT, List.of(item),
            new BigDecimal("300.50"), new BigDecimal("300.50"), now, now, null, null
    );
  }

  private InvoiceDraftDao draftDao(InvoiceDraft draft) {
    InvoiceDraftDao dao = new InvoiceDraftDao();
    dao.setId(draft.id());
    dao.setCompanyId(draft.companyId());
    dao.setCustomerId(draft.customerId());
    dao.setCurrency(draft.currency());
    dao.setStatus(draft.status());
    dao.setSubtotal(draft.subtotal());
    dao.setTotal(draft.total());
    dao.setCreatedAt(draft.createdAt());
    dao.setUpdatedAt(draft.updatedAt());
    dao.setProviderReference(draft.providerReference());
    dao.setIssuedAt(draft.issuedAt());
    return dao;
  }

  private InvoiceItemDao itemDao(InvoiceItem item, UUID draftId) {
    InvoiceItemDao dao = new InvoiceItemDao();
    dao.setId(item.id());
    dao.setInvoiceDraftId(draftId);
    dao.setPosition(0);
    dao.setDescription(item.description());
    dao.setQuantity(item.quantity());
    dao.setUnitPrice(item.unitPrice());
    dao.setLineTotal(item.lineTotal());
    return dao;
  }
}
