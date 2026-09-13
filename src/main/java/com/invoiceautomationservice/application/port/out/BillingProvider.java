package com.invoiceautomationservice.application.port.out;

import com.invoiceautomationservice.domain.model.BillingResult;
import com.invoiceautomationservice.domain.model.InvoiceDraft;

public interface BillingProvider {
  BillingResult issue(InvoiceDraft invoiceDraft);
}
