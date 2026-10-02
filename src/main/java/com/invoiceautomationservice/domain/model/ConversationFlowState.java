package com.invoiceautomationservice.domain.model;

public enum ConversationFlowState {
  EMPTY,
  COLLECTING_DATA,
  PROCESSING_MEDIA,
  NEEDS_REVIEW,
  READY_TO_CREATE,
  DRAFT_CREATED
}
