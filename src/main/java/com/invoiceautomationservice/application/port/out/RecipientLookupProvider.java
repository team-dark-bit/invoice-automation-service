package com.invoiceautomationservice.application.port.out;

import com.invoiceautomationservice.domain.model.IdentityDocumentType;
import com.invoiceautomationservice.domain.model.RecipientLookupResult;

public interface RecipientLookupProvider {
  RecipientLookupResult lookup(IdentityDocumentType documentType, String documentNumber);
}
