package com.invoiceautomationservice.infrastructure.adapter.out.persistence;

import com.invoiceautomationservice.application.port.out.ConversationContextRepository;
import com.invoiceautomationservice.domain.model.ConversationContext;
import com.invoiceautomationservice.domain.model.ConversationDraftItem;
import com.invoiceautomationservice.infrastructure.adapter.out.persistence.entity.ConversationContextEntity;
import com.invoiceautomationservice.infrastructure.adapter.out.persistence.entity.ConversationContextItemEntity;
import com.invoiceautomationservice.infrastructure.adapter.out.persistence.repository.JpaConversationContextItemRepository;
import com.invoiceautomationservice.infrastructure.adapter.out.persistence.repository.JpaConversationContextRepository;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ConversationContextPersistenceAdapter implements ConversationContextRepository {
  private final JpaConversationContextRepository contextRepository;
  private final JpaConversationContextItemRepository itemRepository;

  @Override
  public Optional<ConversationContext> findByConversationId(UUID conversationId) {
    return contextRepository.findById(conversationId).map(entity -> toDomain(entity,
        itemRepository.findAllByConversationIdOrderByPosition(conversationId).stream()
            .map(this::toDomain).toList()));
  }

  @Override
  public ConversationContext save(ConversationContext context) {
    ConversationContextEntity saved = contextRepository.save(toEntity(context));
    itemRepository.deleteAllByConversationId(context.conversationId());
    itemRepository.flush();
    var items = itemRepository.saveAll(IntStream.range(0, context.items().size())
        .mapToObj(index -> toEntity(context.conversationId(), context.items().get(index), index))
        .toList()).stream().map(this::toDomain).toList();
    return toDomain(saved, items);
  }

  private ConversationContextEntity toEntity(ConversationContext value) {
    ConversationContextEntity entity = new ConversationContextEntity();
    entity.setConversationId(value.conversationId());
    entity.setState(value.state());
    entity.setDocumentType(value.documentType());
    entity.setRecipientDocumentType(value.recipientDocumentType());
    entity.setRecipientDocumentNumber(value.recipientDocumentNumber());
    entity.setCurrency(value.currency());
    entity.setInvoiceDraftId(value.invoiceDraftId());
    entity.setUpdatedAt(value.updatedAt());
    return entity;
  }

  private ConversationContextItemEntity toEntity(
      UUID conversationId, ConversationDraftItem value, int position) {
    ConversationContextItemEntity entity = new ConversationContextItemEntity();
    entity.setId(value.id());
    entity.setConversationId(conversationId);
    entity.setPosition(position);
    entity.setDescription(value.description());
    entity.setUnitCode(value.unitCode());
    entity.setQuantity(value.quantity());
    entity.setUnitPrice(value.unitPrice());
    entity.setDiscount(value.discount());
    entity.setTaxAffectation(value.taxAffectation());
    return entity;
  }

  private ConversationContext toDomain(
      ConversationContextEntity value, java.util.List<ConversationDraftItem> items) {
    return new ConversationContext(value.getConversationId(), value.getState(),
        value.getDocumentType(), value.getRecipientDocumentType(),
        value.getRecipientDocumentNumber(), value.getCurrency(), items,
        value.getInvoiceDraftId(), value.getUpdatedAt());
  }

  private ConversationDraftItem toDomain(ConversationContextItemEntity value) {
    return new ConversationDraftItem(value.getId(), value.getDescription(), value.getUnitCode(),
        value.getQuantity(), value.getUnitPrice(), value.getDiscount(), value.getTaxAffectation());
  }
}
