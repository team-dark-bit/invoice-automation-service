package com.invoiceautomationservice.infrastructure.adapter.out.persistence;

import static com.invoiceautomationservice.infrastructure.config.exception.RuntimeErrors.CONVERSATION_NOT_FOUND;

import com.invoiceautomationservice.application.model.PageQuery;
import com.invoiceautomationservice.application.model.PageResult;
import com.invoiceautomationservice.application.port.out.ConversationRepository;
import com.invoiceautomationservice.domain.model.Conversation;
import com.invoiceautomationservice.domain.model.ConversationStatus;
import com.invoiceautomationservice.infrastructure.adapter.out.persistence.entity.ConversationEntity;
import com.invoiceautomationservice.infrastructure.adapter.out.persistence.repository.JpaConversationRepository;
import com.invoiceautomationservice.infrastructure.config.exception.ApplicationException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ConversationPersistenceAdapter implements ConversationRepository {
  private final JpaConversationRepository repository;

  @Override
  public Conversation save(Conversation conversation) {
    return toDomain(repository.save(toEntity(conversation)));
  }

  @Override
  public Conversation findById(UUID id) {
    return repository.findById(id).map(this::toDomain)
        .orElseThrow(() -> new ApplicationException(CONVERSATION_NOT_FOUND, id));
  }

  @Override
  public Conversation findByIdForUpdate(UUID id) {
    return repository.findByIdForUpdate(id).map(this::toDomain)
        .orElseThrow(() -> new ApplicationException(CONVERSATION_NOT_FOUND, id));
  }

  @Override
  public PageResult<Conversation> search(String companyId, ConversationStatus status,
      String externalParticipantId, PageQuery pageQuery) {
    Specification<ConversationEntity> specification =
        (root, query, cb) -> cb.equal(root.get("companyId"), companyId);
    if (status != null) specification = specification.and(
        (root, query, cb) -> cb.equal(root.get("status"), status));
    if (externalParticipantId != null && !externalParticipantId.isBlank()) {
      String value = "%" + externalParticipantId.strip().toLowerCase() + "%";
      specification = specification.and((root, query, cb) ->
          cb.like(cb.lower(root.get("externalParticipantId")), value));
    }
    var page = repository.findAll(specification, PageRequest.of(pageQuery.page(), pageQuery.size(),
        Sort.by(Sort.Direction.DESC, "updatedAt")));
    return new PageResult<>(page.getContent().stream().map(this::toDomain).toList(),
        page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
  }

  private ConversationEntity toEntity(Conversation value) {
    ConversationEntity entity = new ConversationEntity();
    entity.setId(value.id());
    entity.setCompanyId(value.companyId());
    entity.setCustomerId(value.customerId());
    entity.setInvoiceDraftId(value.invoiceDraftId());
    entity.setChannel(value.channel());
    entity.setExternalParticipantId(value.externalParticipantId());
    entity.setStatus(value.status());
    entity.setCreatedAt(value.createdAt());
    entity.setUpdatedAt(value.updatedAt());
    entity.setClosedAt(value.closedAt());
    return entity;
  }

  private Conversation toDomain(ConversationEntity value) {
    return new Conversation(value.getId(), value.getCompanyId(), value.getCustomerId(),
        value.getInvoiceDraftId(), value.getChannel(), value.getExternalParticipantId(),
        value.getStatus(), value.getCreatedAt(), value.getUpdatedAt(), value.getClosedAt());
  }
}
