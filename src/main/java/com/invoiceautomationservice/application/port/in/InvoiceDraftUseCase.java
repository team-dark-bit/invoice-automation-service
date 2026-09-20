package com.invoiceautomationservice.application.port.in;

import com.invoiceautomationservice.application.dto.request.CreateInvoiceDraftRequest;
import com.invoiceautomationservice.application.dto.response.InvoiceDraftResponse;
import com.invoiceautomationservice.application.dto.response.ElectronicDocumentResponse;
import java.util.UUID;

public interface InvoiceDraftUseCase {
  InvoiceDraftResponse create(CreateInvoiceDraftRequest request);
  InvoiceDraftResponse findById(UUID id);
  InvoiceDraftResponse approve(UUID id);
  ElectronicDocumentResponse issue(UUID id);
}
