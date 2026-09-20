package com.invoiceautomationservice.infrastructure.inbound.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.invoiceautomationservice.application.dto.request.CreateInvoiceDraftRequest;
import com.invoiceautomationservice.application.dto.response.InvoiceDraftResponse;
import com.invoiceautomationservice.application.dto.response.InvoiceItemResponse;
import com.invoiceautomationservice.application.dto.response.ElectronicDocumentResponse;
import com.invoiceautomationservice.application.port.in.InvoiceDraftUseCase;
import com.invoiceautomationservice.domain.model.InvoiceDraftStatus;
import com.invoiceautomationservice.domain.exception.InvalidInvoiceDraftStateException;
import com.invoiceautomationservice.infrastructure.adapter.in.web.InvoiceDraftController;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(InvoiceDraftController.class)
class InvoiceDraftControllerTest {

  private static final UUID DRAFT_ID = UUID.fromString("4d773aa2-8ea7-4c26-935d-f47086d7c385");

  @Autowired
  MockMvc mockMvc;
  @MockitoBean
  InvoiceDraftUseCase useCase;

  @Test
  void createsCompleteDraft() throws Exception {
    when(useCase.create(any(CreateInvoiceDraftRequest.class))).thenReturn(response());

    mockMvc.perform(post("/api/v1/invoice-drafts")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(validRequest()))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.statusCode").value(201))
            .andExpect(jsonPath("$.data.id").value(DRAFT_ID.toString()))
            .andExpect(jsonPath("$.data.companyId").value("company-1"))
            .andExpect(jsonPath("$.data.customerId").value("customer-1"))
            .andExpect(jsonPath("$.data.status").value("DRAFT"))
            .andExpect(jsonPath("$.data.items.length()").value(1))
            .andExpect(jsonPath("$.data.subtotal").value(300.5));
  }

  @Test
  void findsCompleteDraft() throws Exception {
    when(useCase.findById(DRAFT_ID)).thenReturn(response());

    mockMvc.perform(get("/api/v1/invoice-drafts/{id}", DRAFT_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("Invoice draft found"))
            .andExpect(jsonPath("$.data.items[0].description").value("Consulting"));
  }

  @Test
  void approvesDraft() throws Exception {
    when(useCase.approve(DRAFT_ID)).thenReturn(withStatus(InvoiceDraftStatus.APPROVED, null, null));

    mockMvc.perform(post("/api/v1/invoice-drafts/{id}/approve", DRAFT_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("Invoice draft approved"))
            .andExpect(jsonPath("$.data.status").value("APPROVED"));
  }

  @Test
  void issuesDraft() throws Exception {
    when(useCase.issue(DRAFT_ID)).thenReturn(electronicDocument());

    mockMvc.perform(post("/api/v1/invoice-drafts/{id}/issue", DRAFT_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("Electronic document issued"))
            .andExpect(jsonPath("$.data.fullNumber").value("B001-00000001"))
            .andExpect(jsonPath("$.data.providerReference").value("MOCK-B001-00000001"));
  }

  @Test
  void returnsConflictForInvalidTransition() throws Exception {
    when(useCase.approve(DRAFT_ID)).thenThrow(
            new InvalidInvoiceDraftStateException(InvoiceDraftStatus.APPROVED, InvoiceDraftStatus.DRAFT)
    );

    mockMvc.perform(post("/api/v1/invoice-drafts/{id}/approve", DRAFT_ID))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.statusCode").value(409))
            .andExpect(jsonPath("$.message").value("Invalid invoice draft state"));
  }

  @Test
  void rejectsDraftWithoutItems() throws Exception {
    mockMvc.perform(post("/api/v1/invoice-drafts")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"companyId":"company-1","documentType":"SALES_RECEIPT",
                             "recipientDocumentType":"DNI","recipientDocumentNumber":"12345678",
                             "currency":"PEN","items":[]}
                            """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Validation failed"))
            .andExpect(jsonPath("$.errors[0]").value("The invoice draft must contain at least one item"));
  }

  @Test
  void rejectsInvalidCurrencyAndItemValues() throws Exception {
    mockMvc.perform(post("/api/v1/invoice-drafts")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"companyId":"company-1","documentType":"SALES_RECEIPT",
                             "recipientDocumentType":"DNI","recipientDocumentNumber":"12345678",
                             "currency":"pen",
                             "items":[{"description":" ","unitCode":"NIU","quantity":0,
                             "unitPrice":-1,"discount":0,"taxAffectation":"TAXED"}]}
                            """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Validation failed"))
            .andExpect(jsonPath("$.errors.length()").value(4));
  }

  private String validRequest() {
    return """
            {"companyId":"company-1","documentType":"SALES_RECEIPT",
             "recipientDocumentType":"DNI","recipientDocumentNumber":"12345678","currency":"PEN",
             "items":[{"description":"Consulting","unitCode":"NIU","quantity":2,
             "unitPrice":150.25,"discount":0,"taxAffectation":"TAXED"}]}
            """;
  }

  private InvoiceDraftResponse response() {
    InvoiceItemResponse item = new InvoiceItemResponse(
            UUID.fromString("bc9fc9f9-dfca-46f0-b6ee-92456183ce7d"), "Consulting",
            new BigDecimal("2"), new BigDecimal("150.25"), new BigDecimal("300.50")
    );
    Instant now = Instant.parse("2026-09-01T10:00:00Z");
    return new InvoiceDraftResponse(
            DRAFT_ID, "company-1", "customer-1",
            com.invoiceautomationservice.domain.model.InvoiceDocumentType.SALES_RECEIPT,
            com.invoiceautomationservice.domain.model.IdentityDocumentType.DNI,
            "12345678", "PEN", InvoiceDraftStatus.DRAFT,
            List.of(item), new BigDecimal("300.50"), new BigDecimal("300.50"), now, now, null, null
    );
  }

  private InvoiceDraftResponse withStatus(
          InvoiceDraftStatus status, String providerReference, Instant issuedAt
  ) {
    InvoiceDraftResponse base = response();
    return new InvoiceDraftResponse(
            base.id(), base.companyId(), base.customerId(), base.documentType(),
            base.recipientDocumentType(), base.recipientDocumentNumber(), base.currency(), status, base.items(),
            base.subtotal(), base.total(), base.createdAt(), base.updatedAt(), providerReference, issuedAt
    );
  }

  private ElectronicDocumentResponse electronicDocument() {
    InvoiceDraftResponse draft = response();
    return new ElectronicDocumentResponse(
        UUID.fromString("7a3ebf16-5d2f-4bc2-8178-56373ed2ee5e"), DRAFT_ID,
        draft.companyId(), draft.customerId(), draft.documentType(), "B001", 1,
        "B001-00000001", draft.recipientDocumentType(), draft.recipientDocumentNumber(),
        draft.currency(), draft.items(), draft.subtotal(), BigDecimal.ZERO,
        new BigDecimal("300.50"), new BigDecimal("54.09"), new BigDecimal("354.59"),
        com.invoiceautomationservice.domain.model.ElectronicDocumentStatus.ACCEPTED,
        "MOCK-B001-00000001", Instant.parse("2026-09-01T11:00:00Z"),
        Instant.parse("2026-09-01T11:00:00Z"), "0", "Accepted");
  }
}
