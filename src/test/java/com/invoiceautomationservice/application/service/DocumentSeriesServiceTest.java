package com.invoiceautomationservice.application.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.invoiceautomationservice.application.dto.request.ConfigureDocumentSeriesRequest;
import com.invoiceautomationservice.application.port.out.DocumentSeriesRepository;
import com.invoiceautomationservice.domain.model.InvoiceDocumentType;
import com.invoiceautomationservice.infrastructure.config.exception.ApplicationException;
import org.junit.jupiter.api.Test;

class DocumentSeriesServiceTest {

  @Test
  void rejectsReceiptSeriesWithInvoicePrefix() {
    DocumentSeriesService service = new DocumentSeriesService(
        mock(DocumentSeriesRepository.class), mock(CompanyAccessService.class),
        mock(AuditTrailService.class));

    assertThatThrownBy(() -> service.configure("company-1",
        new ConfigureDocumentSeriesRequest(InvoiceDocumentType.SALES_RECEIPT, "F001")))
        .isInstanceOf(ApplicationException.class)
        .hasMessage("The series F001 is invalid for document type SALES_RECEIPT");
  }
}
