package com.invoiceautomationservice.application.port.out;

import com.invoiceautomationservice.domain.model.ConversationImage;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConversationImageRepository {
  ConversationImage save(ConversationImage image);
  Optional<ConversationImage> findActiveByConversationIdAndSha256(UUID conversationId, String sha256);
  Optional<ConversationImage> findByIdAndConversationId(UUID imageId, UUID conversationId);
  List<ConversationImage> findExpired(Instant now, int limit);
}
