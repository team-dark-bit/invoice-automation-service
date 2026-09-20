package com.invoiceautomationservice.application.port.out;

import com.invoiceautomationservice.domain.model.BillingResult;
import com.invoiceautomationservice.domain.model.ElectronicDocument;

public interface BillingProvider {
  BillingResult issue(ElectronicDocument document);
}
