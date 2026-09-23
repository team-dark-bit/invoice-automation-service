package com.invoiceautomationservice.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ElectronicDocumentTest {

  @Test
  void snapshotsDraftItemsAndBecomesIssuedWithoutMutatingOriginal() {
    List<InvoiceItem> source = new ArrayList<>(List.of(InvoiceItem.create(
        "Service", UnitCode.NIU, BigDecimal.ONE, new BigDecimal("100"),
        BigDecimal.ZERO, TaxAffectation.TAXED)));
    InvoiceDraft draft = InvoiceDraft.create(
        "company-1", "customer-1", InvoiceDocumentType.SALES_RECEIPT,
        IdentityDocumentType.DNI, "12345678", "PEN", source, Instant.now());
    ElectronicDocument provisional = ElectronicDocument.from(
        draft, new DocumentNumber("B001", 1), issuer(), recipient());
    ElectronicDocument sameDraftDocument = ElectronicDocument.from(
        draft, new DocumentNumber("B001", 1), issuer(), recipient());
    source.clear();

    Instant submittedAt = Instant.parse("2026-09-20T10:00:00Z");
    ElectronicDocument issued = provisional.withBillingResult(new BillingResult(
        "MOCK-B001-00000001", ElectronicDocumentStatus.ACCEPTED,
        submittedAt, submittedAt, "0", "Accepted"));

    assertThat(provisional.providerReference()).isNull();
    assertThat(provisional.id()).isEqualTo(sameDraftDocument.id());
    assertThat(provisional.status()).isEqualTo(ElectronicDocumentStatus.PENDING_SEND);
    assertThat(issued.status()).isEqualTo(ElectronicDocumentStatus.ACCEPTED);
    assertThat(issued.fullNumber()).isEqualTo("B001-00000001");
    assertThat(issued.items()).hasSize(1);
    assertThatThrownBy(() -> issued.items().clear())
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void createsImmutableCreditNoteReferencingOriginalDocument() {
    InvoiceDraft draft = InvoiceDraft.create(
        "company-1", "customer-1", InvoiceDocumentType.SALES_RECEIPT,
        IdentityDocumentType.DNI, "12345678", "PEN",
        List.of(InvoiceItem.create("Service", BigDecimal.ONE, new BigDecimal("100"))),
        Instant.parse("2026-09-23T10:00:00Z"));
    ElectronicDocument original = ElectronicDocument.from(
        draft, new DocumentNumber("B001", 10), issuer(), recipient());

    ElectronicDocument note = ElectronicDocument.noteFrom(
        original, new DocumentNumber("B001", 2), InvoiceDocumentType.CREDIT_NOTE,
        "01", "Anulación de la operación", Instant.parse("2026-09-23T11:00:00Z"));

    assertThat(note.documentType()).isEqualTo(InvoiceDocumentType.CREDIT_NOTE);
    assertThat(note.relatedDocumentId()).isEqualTo(original.id());
    assertThat(note.relatedDocumentType()).isEqualTo(InvoiceDocumentType.SALES_RECEIPT);
    assertThat(note.relatedSeries()).isEqualTo("B001");
    assertThat(note.relatedCorrelative()).isEqualTo(10);
    assertThat(note.noteReasonCode()).isEqualTo("01");
    assertThat(note.total()).isEqualByComparingTo(original.total());
    assertThat(note.items().getFirst().id()).isNotEqualTo(original.items().getFirst().id());
  }

  private IssuerSnapshot issuer() {
    return new IssuerSnapshot(
        "20123456789", "Company SAC", "Company", TaxpayerType.LEGAL_ENTITY,
        "Lima", "150101", "Lima", "Lima", "Lima", "PE");
  }

  private RecipientSnapshot recipient() {
    return new RecipientSnapshot(
        IdentityDocumentType.DNI, "12345678", "Customer", "Lima", "customer@test.pe");
  }
}
