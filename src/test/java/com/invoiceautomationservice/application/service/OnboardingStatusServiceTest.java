package com.invoiceautomationservice.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.invoiceautomationservice.application.port.out.DocumentSeriesRepository;
import com.invoiceautomationservice.application.port.out.IssuerTaxProfileRepository;
import com.invoiceautomationservice.domain.model.DocumentSeries;
import com.invoiceautomationservice.domain.model.InvoiceDocumentType;
import java.util.List;
import org.junit.jupiter.api.Test;

class OnboardingStatusServiceTest {
  @Test
  void reportsCompletedWithTaxProfileAndAtLeastOnePrimarySeries() {
    CompanyAccessService access = mock(CompanyAccessService.class);
    IssuerTaxProfileRepository profiles = mock(IssuerTaxProfileRepository.class);
    DocumentSeriesRepository series = mock(DocumentSeriesRepository.class);
    when(profiles.existsByCompanyId("company-1")).thenReturn(true);
    when(series.findAllByCompanyId("company-1")).thenReturn(List.of(
        new DocumentSeries("series-1", "company-1", InvoiceDocumentType.SALES_RECEIPT,
            "B001", 0, true)));

    var response = new OnboardingStatusService(access, profiles, series).get("company-1");

    verify(access).requireAccess("company-1");
    assertThat(response.completed()).isTrue();
    assertThat(response.salesReceiptSeriesConfigured()).isTrue();
    assertThat(response.pendingSteps()).isEmpty();
  }

  @Test
  void reportsEveryBlockingStep() {
    CompanyAccessService access = mock(CompanyAccessService.class);
    IssuerTaxProfileRepository profiles = mock(IssuerTaxProfileRepository.class);
    DocumentSeriesRepository series = mock(DocumentSeriesRepository.class);
    when(series.findAllByCompanyId("company-1")).thenReturn(List.of());

    var response = new OnboardingStatusService(access, profiles, series).get("company-1");

    assertThat(response.completed()).isFalse();
    assertThat(response.pendingSteps()).containsExactly(
        "CONFIGURE_TAX_PROFILE", "CONFIGURE_PRIMARY_DOCUMENT_SERIES");
  }
}
