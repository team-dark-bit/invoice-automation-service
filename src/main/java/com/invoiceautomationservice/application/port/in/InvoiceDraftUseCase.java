package com.invoiceautomationservice.application.port.in;

import com.invoiceautomationservice.application.dto.request.CreateInvoiceDraftRequest;
import com.invoiceautomationservice.application.dto.request.UpdateInvoiceDraftRequest;
import com.invoiceautomationservice.application.dto.response.InvoiceDraftResponse;
import com.invoiceautomationservice.application.dto.response.ElectronicDocumentResponse;
import java.util.UUID;

public interface InvoiceDraftUseCase {
  InvoiceDraftResponse create(CreateInvoiceDraftRequest request);
  InvoiceDraftResponse findById(UUID id);
  InvoiceDraftResponse update(UUID id, UpdateInvoiceDraftRequest request);
  InvoiceDraftResponse approve(UUID id);
  InvoiceDraftResponse cancel(UUID id);
  ElectronicDocumentResponse issue(UUID id);
}
