package com.invoiceautomationservice.application.port.out;

import com.invoiceautomationservice.domain.model.BillingResult;
import com.invoiceautomationservice.domain.model.BillingSubmission;

public interface BillingProvider {
  BillingResult submit(BillingSubmission submission);

  BillingResult checkStatus(String providerReference);
}
