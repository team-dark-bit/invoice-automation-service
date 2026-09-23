package com.invoiceautomationservice.infrastructure.adapter.out.persistence;

import com.invoiceautomationservice.application.model.PageQuery;
import com.invoiceautomationservice.application.model.PageResult;
import com.invoiceautomationservice.application.port.out.MessageRepository;
import com.invoiceautomationservice.domain.model.Message;
import com.invoiceautomationservice.infrastructure.adapter.out.persistence.entity.MessageEntity;
import com.invoiceautomationservice.infrastructure.adapter.out.persistence.repository.JpaMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class MessagePersistenceAdapter implements MessageRepository {
  private final JpaMessageRepository repository;

  @Override
  public Message save(Message message) {
    return toDomain(repository.save(toEntity(message)));
  }

  @Override
  public boolean existsByConversationIdAndExternalMessageId(
      java.util.UUID conversationId, String externalMessageId) {
    return repository.existsByConversationIdAndExternalMessageId(conversationId, externalMessageId);
  }

  @Override
  public PageResult<Message> findByConversationId(
      java.util.UUID conversationId, PageQuery pageQuery) {
    var page = repository.findAllByConversationId(conversationId, PageRequest.of(
        pageQuery.page(), pageQuery.size(), Sort.by(Sort.Direction.ASC, "createdAt")));
    return new PageResult<>(page.getContent().stream().map(this::toDomain).toList(),
        page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
  }

  private MessageEntity toEntity(Message value) {
    MessageEntity entity = new MessageEntity();
    entity.setId(value.id());
    entity.setConversationId(value.conversationId());
    entity.setDirection(value.direction());
    entity.setType(value.type());
    entity.setContent(value.content());
    entity.setMediaUrl(value.mediaUrl());
    entity.setExternalMessageId(value.externalMessageId());
    entity.setStatus(value.status());
    entity.setCreatedAt(value.createdAt());
    return entity;
  }

  private Message toDomain(MessageEntity value) {
    return new Message(value.getId(), value.getConversationId(), value.getDirection(),
        value.getType(), value.getContent(), value.getMediaUrl(), value.getExternalMessageId(),
        value.getStatus(), value.getCreatedAt());
  }
}
