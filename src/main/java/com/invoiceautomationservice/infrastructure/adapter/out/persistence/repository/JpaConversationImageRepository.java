package com.invoiceautomationservice.infrastructure.adapter.out.persistence.repository;

import com.invoiceautomationservice.infrastructure.adapter.out.persistence.entity.ConversationImageEntity;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaConversationImageRepository
    extends JpaRepository<ConversationImageEntity, UUID> {
  Optional<ConversationImageEntity> findByConversationIdAndSha256AndDeletedAtIsNull(
      UUID conversationId, String sha256);
  Optional<ConversationImageEntity> findByIdAndConversationId(UUID id, UUID conversationId);
  List<ConversationImageEntity> findAllByDeletedAtIsNullAndExpiresAtLessThanEqual(
      Instant now, Pageable pageable);
}
