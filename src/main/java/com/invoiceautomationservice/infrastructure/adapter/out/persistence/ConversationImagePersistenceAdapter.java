package com.invoiceautomationservice.infrastructure.adapter.out.persistence;

import com.invoiceautomationservice.application.port.out.ConversationImageRepository;
import com.invoiceautomationservice.domain.model.ConversationImage;
import com.invoiceautomationservice.domain.model.DocumentInterpretation;
import com.invoiceautomationservice.infrastructure.adapter.out.persistence.entity.ConversationImageEntity;
import com.invoiceautomationservice.infrastructure.adapter.out.persistence.repository.JpaConversationImageRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.json.JsonMapper;

@Repository
@RequiredArgsConstructor
public class ConversationImagePersistenceAdapter implements ConversationImageRepository {
  private final JpaConversationImageRepository repository;
  private final JsonMapper jsonMapper;

  @Override
  public ConversationImage save(ConversationImage image) {
    return toDomain(repository.save(toEntity(image)));
  }

  @Override
  public Optional<ConversationImage> findActiveByConversationIdAndSha256(
      UUID conversationId, String sha256) {
    return repository.findByConversationIdAndSha256AndDeletedAtIsNull(conversationId, sha256)
        .map(this::toDomain);
  }

  @Override
  public Optional<ConversationImage> findByIdAndConversationId(UUID imageId, UUID conversationId) {
    return repository.findByIdAndConversationId(imageId, conversationId).map(this::toDomain);
  }

  @Override
  public Optional<ConversationImage> findByIdForUpdate(UUID imageId) {
    return repository.findByIdForUpdate(imageId).map(this::toDomain);
  }

  @Override
  public List<ConversationImage> findExpired(Instant now, int limit) {
    return repository.findAllByDeletedAtIsNullAndExpiresAtLessThanEqual(
        now, PageRequest.of(0, limit)).stream().map(this::toDomain).toList();
  }

  private ConversationImageEntity toEntity(ConversationImage value) {
    ConversationImageEntity entity = new ConversationImageEntity();
    entity.setId(value.id());
    entity.setConversationId(value.conversationId());
    entity.setMessageId(value.messageId());
    entity.setStorageKey(value.storageKey());
    entity.setOriginalFilename(value.originalFilename());
    entity.setFormat(value.format());
    entity.setSizeBytes(value.sizeBytes());
    entity.setWidth(value.width());
    entity.setHeight(value.height());
    entity.setSha256(value.sha256());
    entity.setRetentionPolicy(value.retentionPolicy());
    entity.setExpiresAt(value.expiresAt());
    entity.setCreatedAt(value.createdAt());
    entity.setDeletedAt(value.deletedAt());
    entity.setProcessingStatus(value.processingStatus());
    entity.setProcessingAttempts(value.processingAttempts());
    entity.setProcessingStartedAt(value.processingStartedAt());
    entity.setProcessedAt(value.processedAt());
    entity.setLastProcessingError(value.lastProcessingError());
    entity.setInterpretationJson(writeInterpretation(value.interpretation()));
    return entity;
  }

  private ConversationImage toDomain(ConversationImageEntity value) {
    return new ConversationImage(value.getId(), value.getConversationId(), value.getMessageId(),
        value.getStorageKey(), value.getOriginalFilename(), value.getFormat(), value.getSizeBytes(),
        value.getWidth(), value.getHeight(), value.getSha256(), value.getRetentionPolicy(),
        value.getExpiresAt(), value.getCreatedAt(), value.getDeletedAt(),
        value.getProcessingStatus(), value.getProcessingAttempts(), value.getProcessingStartedAt(),
        value.getProcessedAt(), value.getLastProcessingError(),
        readInterpretation(value.getInterpretationJson()));
  }

  private String writeInterpretation(DocumentInterpretation interpretation) {
    if (interpretation == null) return null;
    try {
      return jsonMapper.writeValueAsString(interpretation);
    } catch (Exception exception) {
      throw new IllegalStateException("could not serialize image interpretation", exception);
    }
  }

  private DocumentInterpretation readInterpretation(String value) {
    if (value == null || value.isBlank()) return null;
    try {
      return jsonMapper.readValue(value, DocumentInterpretation.class);
    } catch (Exception exception) {
      throw new IllegalStateException("could not deserialize image interpretation", exception);
    }
  }
}
