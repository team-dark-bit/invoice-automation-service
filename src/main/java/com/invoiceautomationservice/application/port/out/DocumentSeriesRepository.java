package com.invoiceautomationservice.application.port.out;

import com.invoiceautomationservice.domain.model.DocumentNumber;
import com.invoiceautomationservice.domain.model.DocumentSeries;
import com.invoiceautomationservice.domain.model.InvoiceDocumentType;
import java.util.List;

public interface DocumentSeriesRepository {
  DocumentSeries save(DocumentSeries series);
  List<DocumentSeries> findAllByCompanyId(String companyId);
  DocumentNumber reserveNext(String companyId, InvoiceDocumentType documentType);
}
