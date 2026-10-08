package com.invoiceautomationservice.application.service;

import com.invoiceautomationservice.application.dto.response.OnboardingStatusResponse;
import com.invoiceautomationservice.application.port.out.DocumentSeriesRepository;
import com.invoiceautomationservice.application.port.out.IssuerTaxProfileRepository;
import com.invoiceautomationservice.domain.model.InvoiceDocumentType;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OnboardingStatusService {
  private final CompanyAccessService companyAccessService;
  private final IssuerTaxProfileRepository taxProfileRepository;
  private final DocumentSeriesRepository seriesRepository;

  @Transactional(readOnly = true)
  public OnboardingStatusResponse get(String companyId) {
    companyAccessService.requireAccess(companyId);
    boolean taxProfile = taxProfileRepository.existsByCompanyId(companyId);
    var activeSeries = seriesRepository.findAllByCompanyId(companyId).stream()
        .filter(series -> series.active()).toList();
    boolean invoiceSeries = activeSeries.stream()
        .anyMatch(series -> series.documentType() == InvoiceDocumentType.INVOICE);
    boolean receiptSeries = activeSeries.stream()
        .anyMatch(series -> series.documentType() == InvoiceDocumentType.SALES_RECEIPT);
    var pending = new ArrayList<String>();
    if (!taxProfile) pending.add("CONFIGURE_TAX_PROFILE");
    if (!invoiceSeries && !receiptSeries) pending.add("CONFIGURE_PRIMARY_DOCUMENT_SERIES");
    return new OnboardingStatusResponse(companyId, true, taxProfile, invoiceSeries, receiptSeries,
        pending.isEmpty(), List.copyOf(pending));
  }
}
