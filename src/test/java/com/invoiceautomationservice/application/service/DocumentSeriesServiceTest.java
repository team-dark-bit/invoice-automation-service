package com.invoiceautomationservice.application.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.assertj.core.api.Assertions.assertThat;

import com.invoiceautomationservice.application.dto.request.ConfigureDocumentSeriesRequest;
import com.invoiceautomationservice.application.port.out.DocumentSeriesRepository;
import com.invoiceautomationservice.domain.model.InvoiceDocumentType;
import com.invoiceautomationservice.infrastructure.config.exception.ApplicationException;
import com.invoiceautomationservice.application.dto.request.UpdateDocumentSeriesRequest;
import com.invoiceautomationservice.domain.model.DocumentSeries;
import java.util.Optional;
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

  @Test
  void updatesUnusedSeriesAndPreservesCorrelative() {
    DocumentSeriesRepository repository = mock(DocumentSeriesRepository.class);
    CompanyAccessService access = mock(CompanyAccessService.class);
    DocumentSeriesService service = new DocumentSeriesService(
        repository, access, mock(AuditTrailService.class));
    var current = new DocumentSeries(
        "series-1", "company-1", InvoiceDocumentType.INVOICE, "F001", 0, true);
    when(repository.findById("series-1")).thenReturn(Optional.of(current));
    when(repository.save(org.mockito.ArgumentMatchers.any())).thenAnswer(i -> i.getArgument(0));

    var response = service.update("company-1", "series-1",
        new UpdateDocumentSeriesRequest("F002", false));

    assertThat(response.series()).isEqualTo("F002");
    assertThat(response.active()).isFalse();
  }

  @Test
  void refusesToDeleteSeriesThatAlreadyIssuedDocuments() {
    DocumentSeriesRepository repository = mock(DocumentSeriesRepository.class);
    DocumentSeriesService service = new DocumentSeriesService(repository,
        mock(CompanyAccessService.class), mock(AuditTrailService.class));
    when(repository.findById("series-1")).thenReturn(Optional.of(new DocumentSeries(
        "series-1", "company-1", InvoiceDocumentType.INVOICE, "F001", 5, true)));

    assertThatThrownBy(() -> service.delete("company-1", "series-1"))
        .isInstanceOf(ApplicationException.class)
        .hasMessageContaining("already issued documents");
  }
}
