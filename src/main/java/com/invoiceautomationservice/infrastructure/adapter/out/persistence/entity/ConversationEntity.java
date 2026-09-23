package com.invoiceautomationservice.infrastructure.adapter.out.persistence.entity;

import com.invoiceautomationservice.domain.model.ConversationChannel;
import com.invoiceautomationservice.domain.model.ConversationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "conversations")
public class ConversationEntity {
  @Id private UUID id;
  @Column(name = "company_id", nullable = false) private String companyId;
  @Column(name = "customer_id") private String customerId;
  @Column(name = "invoice_draft_id") private UUID invoiceDraftId;
  @Enumerated(EnumType.STRING) @Column(nullable = false) private ConversationChannel channel;
  @Column(name = "external_participant_id", length = 150) private String externalParticipantId;
  @Enumerated(EnumType.STRING) @Column(nullable = false) private ConversationStatus status;
  @Column(name = "created_at", nullable = false) private Instant createdAt;
  @Column(name = "updated_at", nullable = false) private Instant updatedAt;
  @Column(name = "closed_at") private Instant closedAt;
}
