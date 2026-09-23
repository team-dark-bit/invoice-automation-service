package com.invoiceautomationservice.application.port.out;

import com.invoiceautomationservice.application.model.PageQuery;
import com.invoiceautomationservice.application.model.PageResult;
import com.invoiceautomationservice.domain.model.Message;
import java.util.UUID;

public interface MessageRepository {
  Message save(Message message);
  boolean existsByConversationIdAndExternalMessageId(UUID conversationId, String externalMessageId);
  PageResult<Message> findByConversationId(UUID conversationId, PageQuery pageQuery);
}
