package com.invoiceautomationservice.application.port.out;

import com.invoiceautomationservice.domain.model.InvoiceDraft;
import java.util.UUID;
import com.invoiceautomationservice.application.model.PageQuery;
import com.invoiceautomationservice.application.model.PageResult;
import com.invoiceautomationservice.domain.model.InvoiceDraftStatus;

public interface InvoiceDraftRepository {
  InvoiceDraft save(InvoiceDraft invoiceDraft);
  InvoiceDraft findById(UUID id);
  InvoiceDraft findByIdForUpdate(UUID id);
  PageResult<InvoiceDraft> search(
      String companyId, InvoiceDraftStatus status, String recipientDocumentNumber, PageQuery page);
}
