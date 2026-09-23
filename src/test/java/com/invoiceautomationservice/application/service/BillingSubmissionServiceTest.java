package com.invoiceautomationservice.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.invoiceautomationservice.application.port.out.BillingProvider;
import com.invoiceautomationservice.domain.model.DocumentNumber;
import com.invoiceautomationservice.domain.model.ElectronicDocument;
import com.invoiceautomationservice.domain.model.IdentityDocumentType;
import com.invoiceautomationservice.domain.model.InvoiceDocumentType;
import com.invoiceautomationservice.domain.model.InvoiceDraft;
import com.invoiceautomationservice.domain.model.InvoiceItem;
import com.invoiceautomationservice.domain.model.IssuerSnapshot;
import com.invoiceautomationservice.domain.model.RecipientSnapshot;
import com.invoiceautomationservice.domain.model.TaxAffectation;
import com.invoiceautomationservice.domain.model.TaxpayerType;
import com.invoiceautomationservice.domain.model.UnitCode;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class BillingSubmissionServiceTest {

  @Test
  void buildsMinimumNubefactTaxContractFromImmutableDocument() {
    Instant emissionAt = Instant.parse("2026-09-23T10:00:00Z");
    InvoiceDraft draft = InvoiceDraft.create(
        "company-1", "customer-1", InvoiceDocumentType.INVOICE,
        IdentityDocumentType.RUC, "20600695771", "USD",
        List.of(
            InvoiceItem.create("Taxed", UnitCode.NIU, BigDecimal.ONE,
                new BigDecimal("100"), BigDecimal.ZERO, TaxAffectation.TAXED),
            InvoiceItem.create("Exempt", UnitCode.ZZ, BigDecimal.ONE,
                new BigDecimal("50"), BigDecimal.ZERO, TaxAffectation.EXEMPT),
            InvoiceItem.create("Unaffected", UnitCode.NIU, BigDecimal.ONE,
                new BigDecimal("25"), BigDecimal.ZERO, TaxAffectation.UNAFFECTED)), emissionAt);
    ElectronicDocument document = ElectronicDocument.from(
        draft, new DocumentNumber("F001", 1),
        new IssuerSnapshot("20123456789", "Issuer SAC", "Issuer",
            TaxpayerType.LEGAL_ENTITY, "Lima", "150101", "Lima", "Lima", "Lima", "PE"),
        new RecipientSnapshot(IdentityDocumentType.RUC, "20600695771", "Customer SAC",
            "Lima", "customer@test.pe"));
    BillingSubmissionService service = new BillingSubmissionService(
        mock(BillingProvider.class), mock(InvoiceIssuancePersistenceService.class),
        Clock.systemUTC());

    var submission = service.toSubmission(document);

    assertThat(submission.documentType().nubefactCode()).isEqualTo("1");
    assertThat(submission.currencyCode()).isEqualTo("2");
    assertThat(submission.emissionAt()).isEqualTo(emissionAt);
    assertThat(submission.taxableTotal()).isEqualByComparingTo("100.00");
    assertThat(submission.exemptTotal()).isEqualByComparingTo("50.00");
    assertThat(submission.unaffectedTotal()).isEqualByComparingTo("25.00");
    assertThat(submission.taxTotal()).isEqualByComparingTo("18.00");
    assertThat(submission.items()).extracting(item -> item.igvTypeCode())
        .containsExactly("1", "8", "9");
    assertThat(submission.items().getFirst().unitPriceWithTax())
        .isEqualByComparingTo("118.00");
  }
}
