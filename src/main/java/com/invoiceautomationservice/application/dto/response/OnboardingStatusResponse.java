package com.invoiceautomationservice.application.dto.response;

import java.util.List;

public record OnboardingStatusResponse(
    String companyId,
    boolean companyRegistered,
    boolean taxProfileConfigured,
    boolean invoiceSeriesConfigured,
    boolean salesReceiptSeriesConfigured,
    boolean completed,
    List<String> pendingSteps
) {}
