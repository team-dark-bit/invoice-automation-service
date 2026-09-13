package com.invoiceautomationservice.domain.exception;

import com.invoiceautomationservice.domain.model.InvoiceDraftStatus;

public class InvalidInvoiceDraftStateException extends RuntimeException {

  public InvalidInvoiceDraftStateException(InvoiceDraftStatus current, InvoiceDraftStatus required) {
    super("Invoice draft must be %s but is %s".formatted(required, current));
  }
}
