package com.invoiceautomationservice.infrastructure.adapter.out.persistence.repository;

import com.invoiceautomationservice.infrastructure.adapter.out.persistence.entity.ConversationImageEntity;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaConversationImageRepository
    extends JpaRepository<ConversationImageEntity, UUID> {
  Optional<ConversationImageEntity> findByConversationIdAndSha256AndDeletedAtIsNull(
      UUID conversationId, String sha256);
  Optional<ConversationImageEntity> findByIdAndConversationId(UUID id, UUID conversationId);
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select image from ConversationImageEntity image where image.id = :id")
  Optional<ConversationImageEntity> findByIdForUpdate(@Param("id") UUID id);
  List<ConversationImageEntity> findAllByDeletedAtIsNullAndExpiresAtLessThanEqual(
      Instant now, Pageable pageable);
}
