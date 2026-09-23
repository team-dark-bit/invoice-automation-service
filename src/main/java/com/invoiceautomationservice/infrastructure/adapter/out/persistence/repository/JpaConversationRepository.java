package com.invoiceautomationservice.infrastructure.adapter.out.persistence.repository;

import com.invoiceautomationservice.infrastructure.adapter.out.persistence.entity.ConversationEntity;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaConversationRepository extends JpaRepository<ConversationEntity, UUID>,
    JpaSpecificationExecutor<ConversationEntity> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select conversation from ConversationEntity conversation where conversation.id = :id")
  Optional<ConversationEntity> findByIdForUpdate(@Param("id") UUID id);
}
