package com.invoiceautomationservice.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.invoiceautomationservice.application.port.out.CustomerRepository;
import com.invoiceautomationservice.application.port.out.RecipientLookupProvider;
import com.invoiceautomationservice.domain.model.Customer;
import com.invoiceautomationservice.domain.model.IdentityDocumentType;
import com.invoiceautomationservice.domain.model.RecipientLookupResult;
import com.invoiceautomationservice.infrastructure.adapter.out.persistence.mapper.dto.CustomerDomainResponseMapper;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class RecipientResolutionServiceTest {

  @Test
  void reusesRecipientAlreadyKnownByCompany() {
    CustomerRepository repository = mock(CustomerRepository.class);
    RecipientLookupProvider provider = mock(RecipientLookupProvider.class);
    Customer existing = new Customer();
    existing.setId("customer-1");
    when(repository.findByCompanyIdAndDocumentTypeAndDocumentNumber(
        "company-1", "DNI", "12345678")).thenReturn(Optional.of(existing));
    RecipientResolutionService service = new RecipientResolutionService(
        repository, provider, mock(CustomerDomainResponseMapper.class));

    assertThat(service.resolve("company-1", IdentityDocumentType.DNI, "12345678"))
        .isSameAs(existing);
    verifyNoInteractions(provider);
  }

  @Test
  void looksUpAndPersistsUnknownRecipientInsideCompany() {
    CustomerRepository repository = mock(CustomerRepository.class);
    RecipientLookupProvider provider = mock(RecipientLookupProvider.class);
    when(repository.findByCompanyIdAndDocumentTypeAndDocumentNumber(
        "company-1", "RUC", "20123456789")).thenReturn(Optional.empty());
    when(provider.lookup(IdentityDocumentType.RUC, "20123456789"))
        .thenReturn(new RecipientLookupResult(null, "Empresa SAC"));
    when(repository.save(org.mockito.ArgumentMatchers.any(Customer.class)))
        .thenAnswer(invocation -> {
          Customer customer = invocation.getArgument(0);
          customer.setId("customer-2");
          return customer;
        });
    RecipientResolutionService service = new RecipientResolutionService(
        repository, provider, mock(CustomerDomainResponseMapper.class));

    Customer result = service.resolve("company-1", IdentityDocumentType.RUC, "20123456789");

    assertThat(result.getId()).isEqualTo("customer-2");
    assertThat(result.getCompanyId()).isEqualTo("company-1");
    assertThat(result.getCompanyName()).isEqualTo("Empresa SAC");
    verify(provider).lookup(IdentityDocumentType.RUC, "20123456789");
  }
}
