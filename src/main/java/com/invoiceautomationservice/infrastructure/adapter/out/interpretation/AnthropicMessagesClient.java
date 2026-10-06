package com.invoiceautomationservice.infrastructure.adapter.out.interpretation;

import java.util.Map;

interface AnthropicMessagesClient {
  String createMessage(Map<String, Object> request);
}
