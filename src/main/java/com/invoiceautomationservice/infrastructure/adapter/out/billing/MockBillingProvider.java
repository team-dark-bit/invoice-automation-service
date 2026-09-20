package com.invoiceautomationservice.infrastructure.adapter.out.billing;

import com.invoiceautomationservice.application.port.out.BillingProvider;
import com.invoiceautomationservice.domain.model.BillingResult;
import com.invoiceautomationservice.domain.model.BillingSubmission;
import com.invoiceautomationservice.domain.model.ElectronicDocumentStatus;
import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "billing", name = "provider", havingValue = "mock", matchIfMissing = true)
public class MockBillingProvider implements BillingProvider {

  private final Clock clock;

  @Override
  public BillingResult submit(BillingSubmission submission) {
    Instant now = Instant.now(clock);
    return new BillingResult(
        "MOCK-" + submission.fullNumber(), ElectronicDocumentStatus.ACCEPTED,
        now, now, "0", "Accepted by mock billing provider");
  }

  @Override
  public BillingResult checkStatus(String providerReference) {
    Instant now = Instant.now(clock);
    return new BillingResult(
        providerReference, ElectronicDocumentStatus.ACCEPTED,
        now, now, "0", "Accepted by mock billing provider");
  }
}
