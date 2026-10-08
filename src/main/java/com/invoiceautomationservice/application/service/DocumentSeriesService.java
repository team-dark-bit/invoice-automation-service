package com.invoiceautomationservice.application.service;

import static com.invoiceautomationservice.infrastructure.config.exception.RuntimeErrors.INVALID_DOCUMENT_SERIES;
import static com.invoiceautomationservice.infrastructure.config.exception.RuntimeErrors.DOCUMENT_SERIES_ALREADY_USED;
import static com.invoiceautomationservice.infrastructure.config.exception.RuntimeErrors.DOCUMENT_SERIES_ID_NOT_FOUND;

import com.invoiceautomationservice.application.dto.request.ConfigureDocumentSeriesRequest;
import com.invoiceautomationservice.application.dto.request.UpdateDocumentSeriesRequest;
import com.invoiceautomationservice.application.dto.response.DocumentSeriesResponse;
import com.invoiceautomationservice.application.port.out.DocumentSeriesRepository;
import com.invoiceautomationservice.domain.model.DocumentSeries;
import com.invoiceautomationservice.domain.model.InvoiceDocumentType;
import com.invoiceautomationservice.domain.model.AuditAction;
import com.invoiceautomationservice.domain.model.CompanyPermission;
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
  private final AuditTrailService auditTrailService;

  @Transactional
  public DocumentSeriesResponse configure(
      String companyId, ConfigureDocumentSeriesRequest request) {
    companyAccessService.requireAccess(companyId);
    companyAccessService.requirePermission(companyId, CompanyPermission.ONBOARDING_MANAGE);
    validatePrefix(request.documentType(), request.series());
    DocumentSeries saved = repository.save(new DocumentSeries(
        UUID.randomUUID().toString(), companyId, request.documentType(), request.series(), 0, true));
    auditTrailService.record(companyId, AuditAction.DOCUMENT_SERIES_CONFIGURED, "DOCUMENT_SERIES",
        saved.id(), "SUCCESS", saved.documentType() + " " + saved.series());
    return toResponse(saved);
  }

  @Transactional(readOnly = true)
  public List<DocumentSeriesResponse> findAll(String companyId) {
    companyAccessService.requireAccess(companyId);
    return repository.findAllByCompanyId(companyId).stream().map(this::toResponse).toList();
  }

  @Transactional
  public DocumentSeriesResponse update(
      String companyId, String seriesId, UpdateDocumentSeriesRequest request) {
    companyAccessService.requireAccess(companyId);
    companyAccessService.requirePermission(companyId, CompanyPermission.ONBOARDING_MANAGE);
    DocumentSeries current = ownedSeries(companyId, seriesId);
    String series = request.series() == null ? current.series() : request.series();
    if (!series.equals(current.series()) && current.currentCorrelative() > 0) {
      throw new ApplicationException(DOCUMENT_SERIES_ALREADY_USED, seriesId);
    }
    validatePrefix(current.documentType(), series);
    boolean active = request.active() == null ? current.active() : request.active();
    DocumentSeries saved = repository.save(new DocumentSeries(current.id(), current.companyId(),
        current.documentType(), series, current.currentCorrelative(), active));
    auditTrailService.record(companyId, AuditAction.DOCUMENT_SERIES_UPDATED, "DOCUMENT_SERIES",
        seriesId, "SUCCESS", saved.documentType() + " " + saved.series());
    return toResponse(saved);
  }

  @Transactional
  public void delete(String companyId, String seriesId) {
    companyAccessService.requireAccess(companyId);
    companyAccessService.requirePermission(companyId, CompanyPermission.ONBOARDING_MANAGE);
    DocumentSeries current = ownedSeries(companyId, seriesId);
    if (current.currentCorrelative() > 0) {
      throw new ApplicationException(DOCUMENT_SERIES_ALREADY_USED, seriesId);
    }
    repository.deleteById(seriesId);
    auditTrailService.record(companyId, AuditAction.DOCUMENT_SERIES_DELETED, "DOCUMENT_SERIES",
        seriesId, "SUCCESS", current.documentType() + " " + current.series());
  }

  private DocumentSeries ownedSeries(String companyId, String seriesId) {
    return repository.findById(seriesId)
        .filter(series -> series.companyId().equals(companyId))
        .orElseThrow(() -> new ApplicationException(DOCUMENT_SERIES_ID_NOT_FOUND, seriesId));
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
