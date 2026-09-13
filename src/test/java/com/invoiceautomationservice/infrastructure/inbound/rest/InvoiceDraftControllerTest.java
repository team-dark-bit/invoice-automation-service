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
import com.invoiceautomationservice.application.port.in.InvoiceDraftUseCase;
import com.invoiceautomationservice.domain.model.InvoiceDraftStatus;
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
  void rejectsDraftWithoutItems() throws Exception {
    mockMvc.perform(post("/api/v1/invoice-drafts")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"companyId":"company-1","customerId":"customer-1","currency":"PEN","items":[]}
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
                            {"companyId":"company-1","customerId":"customer-1","currency":"pen",
                             "items":[{"description":" ","quantity":0,"unitPrice":-1}]}
                            """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Validation failed"))
            .andExpect(jsonPath("$.errors.length()").value(4));
  }

  private String validRequest() {
    return """
            {"companyId":"company-1","customerId":"customer-1","currency":"PEN",
             "items":[{"description":"Consulting","quantity":2,"unitPrice":150.25}]}
            """;
  }

  private InvoiceDraftResponse response() {
    InvoiceItemResponse item = new InvoiceItemResponse(
            UUID.fromString("bc9fc9f9-dfca-46f0-b6ee-92456183ce7d"), "Consulting",
            new BigDecimal("2"), new BigDecimal("150.25"), new BigDecimal("300.50")
    );
    Instant now = Instant.parse("2026-09-01T10:00:00Z");
    return new InvoiceDraftResponse(
            DRAFT_ID, "company-1", "customer-1", "PEN", InvoiceDraftStatus.DRAFT,
            List.of(item), new BigDecimal("300.50"), new BigDecimal("300.50"), now, now
    );
  }
}
