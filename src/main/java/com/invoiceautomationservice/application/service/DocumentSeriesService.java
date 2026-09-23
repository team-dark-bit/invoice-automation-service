package com.invoiceautomationservice.application.service;

import static com.invoiceautomationservice.infrastructure.config.exception.RuntimeErrors.INVALID_DOCUMENT_SERIES;

import com.invoiceautomationservice.application.dto.request.ConfigureDocumentSeriesRequest;
import com.invoiceautomationservice.application.dto.response.DocumentSeriesResponse;
import com.invoiceautomationservice.application.port.out.DocumentSeriesRepository;
import com.invoiceautomationservice.domain.model.DocumentSeries;
import com.invoiceautomationservice.domain.model.InvoiceDocumentType;
import com.invoiceautomationservice.infrastructure.config.exception.ApplicationException;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DocumentSeriesService {

  private final DocumentSeriesRepository repository;
  private final CompanyAccessService companyAccessService;

  @Transactional
  public DocumentSeriesResponse configure(
      String companyId, ConfigureDocumentSeriesRequest request) {
    companyAccessService.requireAccess(companyId);
    validatePrefix(request.documentType(), request.series());
    DocumentSeries saved = repository.save(new DocumentSeries(
        UUID.randomUUID().toString(), companyId, request.documentType(), request.series(), 0, true));
    return toResponse(saved);
  }

  @Transactional(readOnly = true)
  public List<DocumentSeriesResponse> findAll(String companyId) {
    companyAccessService.requireAccess(companyId);
    return repository.findAllByCompanyId(companyId).stream().map(this::toResponse).toList();
  }

  private void validatePrefix(InvoiceDocumentType type, String series) {
    boolean valid = switch (type) {
      case INVOICE -> series.startsWith("F");
      case SALES_RECEIPT -> series.startsWith("B");
      case CREDIT_NOTE, DEBIT_NOTE -> series.startsWith("F") || series.startsWith("B");
    };
    if (!valid) {
      throw new ApplicationException(INVALID_DOCUMENT_SERIES, series, type);
    }
  }

  private DocumentSeriesResponse toResponse(DocumentSeries series) {
    return new DocumentSeriesResponse(
        series.id(), series.companyId(), series.documentType(), series.series(),
        series.currentCorrelative(), series.active());
  }
}
