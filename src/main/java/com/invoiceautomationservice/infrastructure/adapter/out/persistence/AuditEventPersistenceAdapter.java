package com.invoiceautomationservice.infrastructure.adapter.out.persistence;

import com.invoiceautomationservice.application.model.PageQuery;
import com.invoiceautomationservice.application.model.PageResult;
import com.invoiceautomationservice.application.port.out.AuditEventRepository;
import com.invoiceautomationservice.domain.model.AuditAction;
import com.invoiceautomationservice.domain.model.AuditEvent;
import com.invoiceautomationservice.infrastructure.adapter.out.persistence.entity.AuditEventEntity;
import com.invoiceautomationservice.infrastructure.adapter.out.persistence.repository.JpaAuditEventRepository;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class AuditEventPersistenceAdapter implements AuditEventRepository {
  private final JpaAuditEventRepository repository;

  @Override
  public AuditEvent save(AuditEvent event) {
    return toDomain(repository.save(toEntity(event)));
  }

  @Override
  public PageResult<AuditEvent> search(
      String companyId, AuditAction action, String resourceType, String resourceId,
      Instant from, Instant to, PageQuery pageQuery) {
    Specification<AuditEventEntity> specification =
        (root, query, cb) -> cb.equal(root.get("companyId"), companyId);
    if (action != null) specification = specification.and(
        (root, query, cb) -> cb.equal(root.get("action"), action));
    if (resourceType != null && !resourceType.isBlank()) specification = specification.and(
        (root, query, cb) -> cb.equal(root.get("resourceType"), resourceType.strip()));
    if (resourceId != null && !resourceId.isBlank()) specification = specification.and(
        (root, query, cb) -> cb.equal(root.get("resourceId"), resourceId.strip()));
    if (from != null) specification = specification.and(
        (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("occurredAt"), from));
    if (to != null) specification = specification.and(
        (root, query, cb) -> cb.lessThanOrEqualTo(root.get("occurredAt"), to));
    var result = repository.findAll(specification, PageRequest.of(pageQuery.page(),
        pageQuery.size(), Sort.by(Sort.Direction.DESC, "occurredAt")));
    return new PageResult<>(result.getContent().stream().map(this::toDomain).toList(),
        result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
  }

  private AuditEventEntity toEntity(AuditEvent event) {
    return new AuditEventEntity(event.id(), event.companyId(), event.username(), event.action(),
        event.resourceType(), event.resourceId(), event.outcome(), event.detail(), event.occurredAt());
  }

  private AuditEvent toDomain(AuditEventEntity entity) {
    return new AuditEvent(entity.getId(), entity.getCompanyId(), entity.getUsername(),
        entity.getAction(), entity.getResourceType(), entity.getResourceId(), entity.getOutcome(),
        entity.getDetail(), entity.getOccurredAt());
  }
}
