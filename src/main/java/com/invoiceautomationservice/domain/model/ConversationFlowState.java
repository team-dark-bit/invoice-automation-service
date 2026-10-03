package com.invoiceautomationservice.domain.model;

public enum ConversationFlowState {
  EMPTY,
  COLLECTING_DATA,
  PROCESSING_MEDIA,
  NEEDS_REVIEW,
  READY_TO_CREATE,
  AWAITING_DRAFT_CONFIRMATION,
  DRAFT_CREATED
}
