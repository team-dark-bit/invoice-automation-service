package com.invoiceautomationservice.infrastructure.adapter.out.interpretation;

/** Safe technical failure raised by a remote document-understanding adapter. */
public class DocumentUnderstandingProviderException extends RuntimeException {
  public DocumentUnderstandingProviderException(String message) {
    super(message);
  }

  public DocumentUnderstandingProviderException(String message, Throwable cause) {
    super(message, cause);
  }
}
