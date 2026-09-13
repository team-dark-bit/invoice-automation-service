package com.invoiceautomationservice.application.port.out;

import com.invoiceautomationservice.domain.model.InvoiceDraft;
import java.util.UUID;

public interface InvoiceDraftRepository {
  InvoiceDraft save(InvoiceDraft invoiceDraft);
  InvoiceDraft findById(UUID id);
}
