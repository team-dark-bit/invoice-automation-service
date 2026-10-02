package com.invoiceautomationservice.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class DocumentInterpretationTest {

  @Test
  void normalizesAndProtectsExtractedData() {
    var items = new ArrayList<>(List.of(new InterpretedInvoiceItem(
        "  Leche Gloria  ", UnitCode.NIU, new BigDecimal("2"),
        new BigDecimal("3.50"), BigDecimal.ZERO, TaxAffectation.TAXED,
        new BigDecimal("7.00"), new BigDecimal("0.97"), List.of())));
    var missingFields = new ArrayList<>(List.of(" recipientDocumentNumber "));

    DocumentInterpretation result = new DocumentInterpretation(
        InterpretationSource.TEXT, InterpretationIntent.ADD_ITEM,
        InvoiceDocumentType.SALES_RECEIPT, IdentityDocumentType.DNI,
        " 12345678 ", " pen ", items, new BigDecimal("7.00"),
        new BigDecimal("0.95"), missingFields, List.of(), List.of(),
        List.of(" verify recipient "));
    items.clear();
    missingFields.clear();

    assertThat(result.recipientDocumentNumber()).isEqualTo("12345678");
    assertThat(result.currency()).isEqualTo("PEN");
    assertThat(result.items()).singleElement()
        .extracting(InterpretedInvoiceItem::description).isEqualTo("Leche Gloria");
    assertThat(result.missingFields()).containsExactly("recipientDocumentNumber");
    assertThat(result.warnings()).containsExactly("verify recipient");
  }

  @Test
  void rejectsConfidenceOutsideNormalizedRange() {
    assertThatThrownBy(() -> new DocumentInterpretation(
        InterpretationSource.IMAGE, InterpretationIntent.UNKNOWN,
        null, null, null, null, List.of(), null, new BigDecimal("1.01"),
        List.of(), List.of(), List.of(), List.of()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("confidence");
  }

  @Test
  void allowsPartialItemsButRejectsInvalidKnownValues() {
    InterpretedInvoiceItem partial = new InterpretedInvoiceItem(
        "Producto sin precio", null, null, null, null, null,
        null, new BigDecimal("0.40"), List.of("unit price is missing"));

    assertThat(partial.unitPrice()).isNull();
    assertThat(partial.warnings()).containsExactly("unit price is missing");
    assertThatThrownBy(() -> new InterpretedInvoiceItem(
        "Producto", UnitCode.NIU, BigDecimal.ZERO, BigDecimal.ONE,
        BigDecimal.ZERO, TaxAffectation.TAXED, BigDecimal.ZERO,
        BigDecimal.ONE, List.of()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("quantity");
  }
}
