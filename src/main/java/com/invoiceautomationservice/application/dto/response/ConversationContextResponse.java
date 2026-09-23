package com.invoiceautomationservice.application.dto.response;

import com.invoiceautomationservice.domain.model.ConversationFlowState;
import com.invoiceautomationservice.domain.model.IdentityDocumentType;
import com.invoiceautomationservice.domain.model.InvoiceDocumentType;
import java.util.UUID;

public record ConversationContextResponse(
    ConversationFlowState state, InvoiceDocumentType documentType,
    IdentityDocumentType recipientDocumentType, String recipientDocumentNumber,
    String currency, int itemCount, UUID invoiceDraftId
) {}
