package com.invoiceautomationservice.application.service;

import static com.invoiceautomationservice.infrastructure.config.exception.RuntimeErrors.INVALID_RECIPIENT_DOCUMENT;

import com.invoiceautomationservice.application.dto.response.CustomerResponse;
import com.invoiceautomationservice.application.port.out.CustomerRepository;
import com.invoiceautomationservice.application.port.out.RecipientLookupProvider;
import com.invoiceautomationservice.domain.model.Customer;
import com.invoiceautomationservice.domain.model.IdentityDocumentType;
import com.invoiceautomationservice.domain.model.RecipientLookupResult;
import com.invoiceautomationservice.infrastructure.adapter.out.persistence.mapper.dto.CustomerDomainResponseMapper;
import com.invoiceautomationservice.infrastructure.config.exception.ApplicationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RecipientResolutionService {

  private final CustomerRepository customerRepository;
  private final RecipientLookupProvider lookupProvider;
  private final CustomerDomainResponseMapper responseMapper;

  @Transactional
  public Customer resolve(String companyId, IdentityDocumentType documentType, String documentNumber) {
    validate(documentType, documentNumber);
    return customerRepository
        .findByCompanyIdAndDocumentTypeAndDocumentNumber(
            companyId, documentType.name(), documentNumber)
        .orElseGet(() -> create(companyId, documentType, documentNumber));
  }

  public CustomerResponse resolveResponse(
      String companyId, IdentityDocumentType documentType, String documentNumber) {
    return responseMapper.toResponse(resolve(companyId, documentType, documentNumber));
  }

  private Customer create(
      String companyId, IdentityDocumentType documentType, String documentNumber) {
    RecipientLookupResult lookup = lookupProvider.lookup(documentType, documentNumber);
    Customer customer = new Customer();
    customer.setCompanyId(companyId);
    customer.setDocumentType(documentType.name());
    customer.setDocumentNumber(documentNumber);
    customer.setFullName(lookup.fullName());
    customer.setCompanyName(lookup.companyName());
    customer.setAddress(lookup.address());
    customer.setEmail(lookup.email());
    customer.setActive(true);
    return customerRepository.save(customer);
  }

  private void validate(IdentityDocumentType documentType, String documentNumber) {
    if (documentType == null
        || documentNumber == null
        || !documentNumber.matches("\\d{" + documentType.length() + "}")) {
      String type = documentType == null ? "UNKNOWN" : documentType.name();
      int length = documentType == null ? 0 : documentType.length();
      throw new ApplicationException(INVALID_RECIPIENT_DOCUMENT, type, length);
    }
  }
}
