package com.invoiceautomationservice.application.port.out;

import com.invoiceautomationservice.domain.model.ConversationContext;
import java.util.Optional;
import java.util.UUID;

public interface ConversationContextRepository {
  Optional<ConversationContext> findByConversationId(UUID conversationId);
  ConversationContext save(ConversationContext context);
}
