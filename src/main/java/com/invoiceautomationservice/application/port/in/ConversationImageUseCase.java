package com.invoiceautomationservice.application.port.in;

import com.invoiceautomationservice.application.dto.response.ConversationImageResponse;
import com.invoiceautomationservice.application.model.ConversationImageContent;
import com.invoiceautomationservice.application.model.UploadConversationImageCommand;
import java.util.UUID;

public interface ConversationImageUseCase {
  ConversationImageResponse upload(UUID conversationId, UploadConversationImageCommand command);
  ConversationImageResponse findById(UUID conversationId, UUID imageId);
  ConversationImageContent loadContent(UUID conversationId, UUID imageId);
  void delete(UUID conversationId, UUID imageId);
}
