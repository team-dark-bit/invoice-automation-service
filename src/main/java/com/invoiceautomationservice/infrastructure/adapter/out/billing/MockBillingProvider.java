package com.invoiceautomationservice.infrastructure.adapter.out.billing;

import com.invoiceautomationservice.application.port.out.BillingProvider;
import com.invoiceautomationservice.domain.model.BillingResult;
import com.invoiceautomationservice.domain.model.InvoiceDraft;
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
  public BillingResult issue(InvoiceDraft invoiceDraft) {
    return new BillingResult("MOCK-" + invoiceDraft.id(), Instant.now(clock));
  }
}
