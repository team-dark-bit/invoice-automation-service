package com.invoiceautomationservice.application.port.in;

import com.invoiceautomationservice.application.dto.response.ConversationEngineResponse;
import java.util.UUID;

public interface ConversationEngineUseCase {
  ConversationEngineResponse process(UUID conversationId, String text, String externalMessageId);
}
