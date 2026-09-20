package com.invoiceautomationservice.infrastructure.adapter.out.persistence;

import static com.invoiceautomationservice.infrastructure.config.exception.RuntimeErrors.DOCUMENT_SERIES_NOT_FOUND;

import com.invoiceautomationservice.application.port.out.DocumentSeriesRepository;
import com.invoiceautomationservice.domain.model.DocumentNumber;
import com.invoiceautomationservice.domain.model.DocumentSeries;
import com.invoiceautomationservice.domain.model.InvoiceDocumentType;
import com.invoiceautomationservice.infrastructure.adapter.out.persistence.entity.DocumentSeriesEntity;
import com.invoiceautomationservice.infrastructure.adapter.out.persistence.repository.JpaDocumentSeriesRepository;
import com.invoiceautomationservice.infrastructure.config.exception.ApplicationException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class DocumentSeriesPersistenceAdapter implements DocumentSeriesRepository {

  private final JpaDocumentSeriesRepository repository;

  @Override
  public DocumentSeries save(DocumentSeries series) {
    return toDomain(repository.save(toEntity(series)));
  }

  @Override
  public List<DocumentSeries> findAllByCompanyId(String companyId) {
    return repository.findAllByCompanyIdOrderBySeries(companyId).stream()
        .map(this::toDomain).toList();
  }

  @Override
  public DocumentNumber reserveNext(String companyId, InvoiceDocumentType documentType) {
    DocumentSeriesEntity entity = repository
        .findFirstByCompanyIdAndDocumentTypeAndActiveTrueOrderBySeries(companyId, documentType)
        .orElseThrow(() -> new ApplicationException(
            DOCUMENT_SERIES_NOT_FOUND, documentType, companyId));
    long next = entity.getCurrentCorrelative() + 1;
    entity.setCurrentCorrelative(next);
    repository.save(entity);
    return new DocumentNumber(entity.getSeries(), next);
  }

  private DocumentSeriesEntity toEntity(DocumentSeries series) {
    DocumentSeriesEntity entity = new DocumentSeriesEntity();
    entity.setId(series.id());
    entity.setCompanyId(series.companyId());
    entity.setDocumentType(series.documentType());
    entity.setSeries(series.series());
    entity.setCurrentCorrelative(series.currentCorrelative());
    entity.setActive(series.active());
    // A null version tells Hibernate that this UUID-backed entity is new.
    entity.setVersion(null);
    return entity;
  }

  private DocumentSeries toDomain(DocumentSeriesEntity entity) {
    return new DocumentSeries(
        entity.getId(), entity.getCompanyId(), entity.getDocumentType(), entity.getSeries(),
        entity.getCurrentCorrelative(), entity.getActive());
  }
}
