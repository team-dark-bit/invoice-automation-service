package com.invoiceautomationservice.infrastructure.adapter.out.persistence.entity;

import com.invoiceautomationservice.domain.model.ConversationFlowState;
import com.invoiceautomationservice.domain.model.IdentityDocumentType;
import com.invoiceautomationservice.domain.model.InvoiceDocumentType;
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
@Table(name = "conversation_contexts")
public class ConversationContextEntity {
  @Id @Column(name = "conversation_id") private UUID conversationId;
  @Enumerated(EnumType.STRING) @Column(nullable = false) private ConversationFlowState state;
  @Enumerated(EnumType.STRING) @Column(name = "document_type") private InvoiceDocumentType documentType;
  @Enumerated(EnumType.STRING) @Column(name = "recipient_document_type")
  private IdentityDocumentType recipientDocumentType;
  @Column(name = "recipient_document_number", length = 20) private String recipientDocumentNumber;
  @Column(nullable = false, length = 3) private String currency;
  @Column(name = "invoice_draft_id") private UUID invoiceDraftId;
  @Column(name = "updated_at", nullable = false) private Instant updatedAt;
}
