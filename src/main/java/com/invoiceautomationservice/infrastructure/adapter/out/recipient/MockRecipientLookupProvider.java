package com.invoiceautomationservice.infrastructure.adapter.out.recipient;

import com.invoiceautomationservice.application.port.out.RecipientLookupProvider;
import com.invoiceautomationservice.domain.model.IdentityDocumentType;
import com.invoiceautomationservice.domain.model.RecipientLookupResult;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
    prefix = "recipient",
    name = "lookup-provider",
    havingValue = "mock",
    matchIfMissing = true)
public class MockRecipientLookupProvider implements RecipientLookupProvider {

  @Override
  public RecipientLookupResult lookup(
      IdentityDocumentType documentType, String documentNumber) {
    if (documentType == IdentityDocumentType.RUC) {
      return new RecipientLookupResult(null, "Empresa RUC " + documentNumber, null, null);
    }
    return new RecipientLookupResult("Persona DNI " + documentNumber, null, null, null);
  }
}
