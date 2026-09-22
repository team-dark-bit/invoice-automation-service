package com.invoiceautomationservice.application.service;

import com.invoiceautomationservice.domain.model.ElectronicDocument;

public record PreparedEmission(ElectronicDocument document, boolean submitRequired) {
}
