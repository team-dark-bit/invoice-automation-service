package com.invoiceautomationservice.infrastructure.adapter.out.persistence.repository;

import com.invoiceautomationservice.infrastructure.adapter.out.persistence.entity.ConversationContextItemEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaConversationContextItemRepository
    extends JpaRepository<ConversationContextItemEntity, UUID> {
  List<ConversationContextItemEntity> findAllByConversationIdOrderByPosition(UUID conversationId);
  void deleteAllByConversationId(UUID conversationId);
}
