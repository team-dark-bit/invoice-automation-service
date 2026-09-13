package com.invoiceautomationservice.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.invoiceautomationservice.application.dto.request.CreateInvoiceDraftRequest;
import com.invoiceautomationservice.application.dto.request.CreateInvoiceItemRequest;
import com.invoiceautomationservice.application.dto.response.InvoiceDraftResponse;
import com.invoiceautomationservice.application.dto.response.InvoiceItemResponse;
import com.invoiceautomationservice.application.port.out.CompanyRepository;
import com.invoiceautomationservice.application.port.out.BillingProvider;
import com.invoiceautomationservice.application.port.out.CustomerRepository;
import com.invoiceautomationservice.application.port.out.InvoiceDraftRepository;
import com.invoiceautomationservice.application.service.mapper.InvoiceDraftDomainResponseMapper;
import com.invoiceautomationservice.domain.model.Company;
import com.invoiceautomationservice.domain.model.BillingResult;
import com.invoiceautomationservice.domain.model.Customer;
import com.invoiceautomationservice.domain.model.InvoiceDraft;
import com.invoiceautomationservice.domain.model.InvoiceItem;
import com.invoiceautomationservice.infrastructure.config.exception.ApplicationException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CreateInvoiceDraftServiceTest {

  private InvoiceDraftRepository draftRepository;
  private CompanyRepository companyRepository;
  private CustomerRepository customerRepository;
  private InvoiceDraftDomainResponseMapper mapper;
  private BillingProvider billingProvider;
  private InvoiceDraftService service;

  @BeforeEach
  void setUp() {
    draftRepository = mock(InvoiceDraftRepository.class);
    companyRepository = mock(CompanyRepository.class);
    customerRepository = mock(CustomerRepository.class);
    mapper = mock(InvoiceDraftDomainResponseMapper.class);
    billingProvider = mock(BillingProvider.class);
    Clock clock = Clock.fixed(Instant.parse("2026-09-01T10:00:00Z"), ZoneOffset.UTC);
    service = new InvoiceDraftService(
            draftRepository, companyRepository, customerRepository, billingProvider, mapper, clock
    );
  }

  @Test
  void createsCompleteDraftAndCalculatesTotals() {
    CreateInvoiceDraftRequest request = request();
    when(companyRepository.findById("company-1")).thenReturn(company(true));
    when(customerRepository.findById("customer-1")).thenReturn(customer(true));
    when(draftRepository.save(any(InvoiceDraft.class))).thenAnswer(invocation -> invocation.getArgument(0));
    when(mapper.toResponse(any(InvoiceDraft.class))).thenAnswer(invocation -> response(invocation.getArgument(0)));

    InvoiceDraftResponse result = service.create(request);

    assertThat(result.id()).isNotNull();
    assertThat(result.companyId()).isEqualTo("company-1");
    assertThat(result.customerId()).isEqualTo("customer-1");
    assertThat(result.currency()).isEqualTo("PEN");
    assertThat(result.items()).hasSize(2);
    assertThat(result.subtotal()).isEqualByComparingTo("350.50");
    assertThat(result.total()).isEqualByComparingTo("350.50");
    assertThat(result.createdAt()).isEqualTo(Instant.parse("2026-09-01T10:00:00Z"));
    verify(draftRepository).save(any(InvoiceDraft.class));
  }

  @Test
  void rejectsInactiveCompany() {
    when(companyRepository.findById("company-1")).thenReturn(company(false));

    assertThatThrownBy(() -> service.create(request()))
            .isInstanceOf(ApplicationException.class)
            .hasMessage("The company with id: company-1 is inactive");
  }

  @Test
  void rejectsInactiveCustomer() {
    when(companyRepository.findById("company-1")).thenReturn(company(true));
    when(customerRepository.findById("customer-1")).thenReturn(customer(false));

    assertThatThrownBy(() -> service.create(request()))
            .isInstanceOf(ApplicationException.class)
            .hasMessage("The customer with id: customer-1 is inactive");
  }

  @Test
  void findsCompleteDraft() {
    InvoiceDraft draft = draft();
    InvoiceDraftResponse response = response(draft);
    when(draftRepository.findById(draft.id())).thenReturn(draft);
    when(mapper.toResponse(draft)).thenReturn(response);

    assertThat(service.findById(draft.id())).isSameAs(response);
  }

  @Test
  void approvesDraftAndPersistsTransition() {
    InvoiceDraft draft = draft();
    when(draftRepository.findById(draft.id())).thenReturn(draft);
    when(draftRepository.save(any(InvoiceDraft.class))).thenAnswer(invocation -> invocation.getArgument(0));
    when(mapper.toResponse(any(InvoiceDraft.class))).thenAnswer(invocation -> response(invocation.getArgument(0)));

    InvoiceDraftResponse result = service.approve(draft.id());

    assertThat(result.status()).isEqualTo(com.invoiceautomationservice.domain.model.InvoiceDraftStatus.APPROVED);
    verify(draftRepository).save(any(InvoiceDraft.class));
  }

  @Test
  void issuesApprovedDraftThroughBillingProvider() {
    InvoiceDraft approved = draft().approve(Instant.parse("2026-09-01T09:00:00Z"));
    BillingResult billingResult = new BillingResult(
            "MOCK-" + approved.id(), Instant.parse("2026-09-01T10:00:00Z")
    );
    when(draftRepository.findById(approved.id())).thenReturn(approved);
    when(billingProvider.issue(approved)).thenReturn(billingResult);
    when(draftRepository.save(any(InvoiceDraft.class))).thenAnswer(invocation -> invocation.getArgument(0));
    when(mapper.toResponse(any(InvoiceDraft.class))).thenAnswer(invocation -> response(invocation.getArgument(0)));

    InvoiceDraftResponse result = service.issue(approved.id());

    assertThat(result.status()).isEqualTo(com.invoiceautomationservice.domain.model.InvoiceDraftStatus.ISSUED);
    assertThat(result.providerReference()).isEqualTo("MOCK-" + approved.id());
    assertThat(result.issuedAt()).isEqualTo(Instant.parse("2026-09-01T10:00:00Z"));
    verify(billingProvider).issue(approved);
  }

  @Test
  void doesNotCallBillingProviderWhenDraftIsNotApproved() {
    InvoiceDraft draft = draft();
    when(draftRepository.findById(draft.id())).thenReturn(draft);

    assertThatThrownBy(() -> service.issue(draft.id()))
            .isInstanceOf(com.invoiceautomationservice.domain.exception.InvalidInvoiceDraftStateException.class);
    verifyNoInteractions(billingProvider);
  }

  private CreateInvoiceDraftRequest request() {
    return new CreateInvoiceDraftRequest("company-1", "customer-1", "PEN", List.of(
            new CreateInvoiceItemRequest("Consulting", new BigDecimal("2"), new BigDecimal("150.25")),
            new CreateInvoiceItemRequest("Support", BigDecimal.ONE, new BigDecimal("50.00"))
    ));
  }

  private InvoiceDraft draft() {
    return InvoiceDraft.create("company-1", "customer-1", "PEN", List.of(
            InvoiceItem.create("Consulting", new BigDecimal("2"), new BigDecimal("150.25")),
            InvoiceItem.create("Support", BigDecimal.ONE, new BigDecimal("50.00"))
    ), Instant.parse("2026-09-01T10:00:00Z"));
  }

  private Company company(boolean active) {
    Company company = new Company();
    company.setId("company-1");
    company.setActive(active);
    return company;
  }

  private Customer customer(boolean active) {
    Customer customer = new Customer();
    customer.setId("customer-1");
    customer.setActive(active);
    return customer;
  }

  private InvoiceDraftResponse response(InvoiceDraft draft) {
    List<InvoiceItemResponse> items = draft.items().stream().map(item -> new InvoiceItemResponse(
            item.id(), item.description(), item.quantity(), item.unitPrice(), item.lineTotal()
    )).toList();
    return new InvoiceDraftResponse(
            draft.id(), draft.companyId(), draft.customerId(), draft.currency(), draft.status(),
            items, draft.subtotal(), draft.total(), draft.createdAt(), draft.updatedAt(),
            draft.providerReference(), draft.issuedAt()
    );
  }
}
