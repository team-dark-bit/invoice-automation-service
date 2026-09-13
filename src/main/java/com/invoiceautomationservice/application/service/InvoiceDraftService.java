package com.invoiceautomationservice.application.service;

import static com.invoiceautomationservice.infrastructure.config.exception.RuntimeErrors.COMPANY_INACTIVE;
import static com.invoiceautomationservice.infrastructure.config.exception.RuntimeErrors.CUSTOMER_INACTIVE;

import com.invoiceautomationservice.application.dto.request.CreateInvoiceDraftRequest;
import com.invoiceautomationservice.application.dto.response.InvoiceDraftResponse;
import com.invoiceautomationservice.application.port.in.InvoiceDraftUseCase;
import com.invoiceautomationservice.application.port.out.CompanyRepository;
import com.invoiceautomationservice.application.port.out.CustomerRepository;
import com.invoiceautomationservice.application.port.out.InvoiceDraftRepository;
import com.invoiceautomationservice.application.service.mapper.InvoiceDraftDomainResponseMapper;
import com.invoiceautomationservice.domain.model.Company;
import com.invoiceautomationservice.domain.model.Customer;
import com.invoiceautomationservice.domain.model.InvoiceDraft;
import com.invoiceautomationservice.domain.model.InvoiceItem;
import com.invoiceautomationservice.infrastructure.config.exception.ApplicationException;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InvoiceDraftService implements InvoiceDraftUseCase {

  private final InvoiceDraftRepository invoiceDraftRepository;
  private final CompanyRepository companyRepository;
  private final CustomerRepository customerRepository;
  private final InvoiceDraftDomainResponseMapper responseMapper;
  private final Clock clock;

  @Override
  @Transactional
  public InvoiceDraftResponse create(CreateInvoiceDraftRequest request) {
    Company company = companyRepository.findById(request.companyId());
    if (!company.isActive()) {
      throw new ApplicationException(COMPANY_INACTIVE, request.companyId());
    }
    Customer customer = customerRepository.findById(request.customerId());
    if (!customer.isActive()) {
      throw new ApplicationException(CUSTOMER_INACTIVE, request.customerId());
    }

    List<InvoiceItem> items = request.items().stream()
            .map(item -> InvoiceItem.create(item.description(), item.quantity(), item.unitPrice()))
            .toList();
    InvoiceDraft draft = InvoiceDraft.create(
            request.companyId(), request.customerId(), request.currency(), items, Instant.now(clock)
    );
    return responseMapper.toResponse(invoiceDraftRepository.save(draft));
  }

  @Override
  @Transactional(readOnly = true)
  public InvoiceDraftResponse findById(UUID id) {
    return responseMapper.toResponse(invoiceDraftRepository.findById(id));
  }
}
