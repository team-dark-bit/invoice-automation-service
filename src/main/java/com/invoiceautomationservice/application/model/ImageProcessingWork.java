package com.invoiceautomationservice.application.model;

import com.invoiceautomationservice.domain.model.ConversationImage;
import com.invoiceautomationservice.domain.model.InterpretationContextSnapshot;

public record ImageProcessingWork(
    ConversationImage image, String companyId, InterpretationContextSnapshot context
) {}
