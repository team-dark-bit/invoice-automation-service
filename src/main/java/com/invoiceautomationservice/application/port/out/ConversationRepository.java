package com.invoiceautomationservice.application.port.out;

import com.invoiceautomationservice.application.model.PageQuery;
import com.invoiceautomationservice.application.model.PageResult;
import com.invoiceautomationservice.domain.model.Conversation;
import com.invoiceautomationservice.domain.model.ConversationStatus;
import java.util.UUID;

public interface ConversationRepository {
  Conversation save(Conversation conversation);
  Conversation findById(UUID id);
  Conversation findByIdForUpdate(UUID id);
  PageResult<Conversation> search(String companyId, ConversationStatus status,
      String externalParticipantId, PageQuery pageQuery);
}
