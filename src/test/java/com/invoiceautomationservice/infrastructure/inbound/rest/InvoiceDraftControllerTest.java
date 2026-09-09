package com.invoiceautomationservice.infrastructure.inbound.rest;
import com.invoiceautomationservice.application.port.in.CreateInvoiceDraftUseCase;
import com.invoiceautomationservice.domain.model.InvoiceDraft;
import com.invoiceautomationservice.domain.model.InvoiceDraftStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.time.Instant;
import java.util.UUID;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@WebMvcTest(InvoiceDraftController.class)
class InvoiceDraftControllerTest {
    @Autowired MockMvc mockMvc;
    @MockitoBean CreateInvoiceDraftUseCase useCase;
    @Test void createsDraft() throws Exception {
        var id = UUID.fromString("4d773aa2-8ea7-4c26-935d-f47086d7c385");
        var createdAt = Instant.parse("2026-09-01T10:00:00Z");
        when(useCase.create()).thenReturn(new InvoiceDraft(id, InvoiceDraftStatus.DRAFT, createdAt));
        mockMvc.perform(post("/api/v1/invoice-drafts"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.createdAt").value("2026-09-01T10:00:00Z"));
    }
}
