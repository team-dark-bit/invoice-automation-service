package com.invoiceautomationservice.application.port.out;

import com.invoiceautomationservice.domain.model.DocumentNumber;
import com.invoiceautomationservice.domain.model.DocumentSeries;
import com.invoiceautomationservice.domain.model.InvoiceDocumentType;
import java.util.List;
import java.util.Optional;

public interface DocumentSeriesRepository {
  DocumentSeries save(DocumentSeries series);
  List<DocumentSeries> findAllByCompanyId(String companyId);
  Optional<DocumentSeries> findById(String id);
  void deleteById(String id);
  DocumentNumber reserveNext(String companyId, InvoiceDocumentType documentType);
  DocumentNumber reserveNext(
      String companyId, InvoiceDocumentType documentType, String seriesPrefix);
}
