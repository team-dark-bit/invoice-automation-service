package com.invoiceautomationservice.infrastructure.adapter.out.persistence;

import static com.invoiceautomationservice.infrastructure.config.exception.RuntimeErrors.INVOICE_DRAFT_NOT_FOUND;

import com.invoiceautomationservice.application.port.out.InvoiceDraftRepository;
import com.invoiceautomationservice.domain.model.InvoiceDraft;
import com.invoiceautomationservice.domain.model.InvoiceItem;
import com.invoiceautomationservice.infrastructure.adapter.out.persistence.entity.InvoiceDraftEntity;
import com.invoiceautomationservice.infrastructure.adapter.out.persistence.mapper.domain.InvoiceDraftDaoDomainMapper;
import com.invoiceautomationservice.infrastructure.adapter.out.persistence.mapper.domain.InvoiceItemDaoDomainMapper;
import com.invoiceautomationservice.infrastructure.adapter.out.persistence.repository.JpaInvoiceDraftRepository;
import com.invoiceautomationservice.infrastructure.adapter.out.persistence.repository.JpaInvoiceItemRepository;
import com.invoiceautomationservice.infrastructure.config.exception.ApplicationException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.UUID;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class InvoiceDraftPersistenceAdapter implements InvoiceDraftRepository {

  private final JpaInvoiceDraftRepository draftRepository;
  private final JpaInvoiceItemRepository itemRepository;
  private final InvoiceDraftDaoDomainMapper draftMapper;
  private final InvoiceItemDaoDomainMapper itemMapper;

  @Override
  public InvoiceDraft save(InvoiceDraft draft) {
    InvoiceDraftEntity savedDraft = draftRepository.save(draftMapper.toDao(draft));
    Set<UUID> currentItemIds = draft.items().stream()
        .map(InvoiceItem::id)
        .collect(Collectors.toSet());
    var obsoleteItems = itemRepository.findAllByInvoiceDraftIdOrderByPosition(draft.id()).stream()
        .filter(item -> !currentItemIds.contains(item.getId()))
        .toList();
    if (!obsoleteItems.isEmpty()) {
      itemRepository.deleteAll(obsoleteItems);
      itemRepository.flush();
    }
    List<InvoiceItem> savedItems = itemRepository.saveAll(
            IntStream.range(0, draft.items().size())
                    .mapToObj(index -> itemMapper.toDao(draft.items().get(index), draft.id(), index))
                    .toList()
    ).stream().map(itemMapper::toDomain).toList();
    return toDomain(savedDraft, savedItems);
  }

  @Override
  public InvoiceDraft findById(UUID id) {
    InvoiceDraftEntity draft = draftRepository.findById(id)
            .orElseThrow(() -> new ApplicationException(INVOICE_DRAFT_NOT_FOUND, id));
    List<InvoiceItem> items = itemRepository.findAllByInvoiceDraftIdOrderByPosition(id).stream()
            .map(itemMapper::toDomain)
            .toList();
    return toDomain(draft, items);
  }

  @Override
  public InvoiceDraft findByIdForUpdate(UUID id) {
    InvoiceDraftEntity draft = draftRepository.findByIdForUpdate(id)
        .orElseThrow(() -> new ApplicationException(INVOICE_DRAFT_NOT_FOUND, id));
    List<InvoiceItem> items = itemRepository.findAllByInvoiceDraftIdOrderByPosition(id).stream()
        .map(itemMapper::toDomain)
        .toList();
    return toDomain(draft, items);
  }

  private InvoiceDraft toDomain(InvoiceDraftEntity draft, List<InvoiceItem> items) {
    return new InvoiceDraft(
            draft.getId(), draft.getCompanyId(), draft.getCustomerId(), draft.getDocumentType(),
            draft.getRecipientDocumentType(), draft.getRecipientDocumentNumber(), draft.getCurrency(),
            draft.getStatus(), items, draft.getSubtotal(), draft.getDiscountTotal(),
            draft.getTaxableTotal(), draft.getTaxTotal(), draft.getTotal(),
            draft.getCreatedAt(), draft.getUpdatedAt(), draft.getProviderReference(), draft.getIssuedAt()
    );
  }
}
