package com.invoiceautomationservice.infrastructure.adapter.out.interpretation;

import static org.assertj.core.api.Assertions.assertThat;

import com.invoiceautomationservice.domain.model.IdentityDocumentType;
import com.invoiceautomationservice.domain.model.InterpretationContextSnapshot;
import com.invoiceautomationservice.domain.model.InterpretationIntent;
import com.invoiceautomationservice.domain.model.InvoiceDocumentType;
import com.invoiceautomationservice.domain.model.TextInterpretationInput;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RuleBasedDocumentUnderstandingProviderTest {
  private final RuleBasedDocumentUnderstandingProvider provider =
      new RuleBasedDocumentUnderstandingProvider();

  @Test
  void interpretsNaturalReceiptHeader() {
    var result = provider.interpretText(input(
        "Quiero una boleta para DNI 12345678", InterpretationContextSnapshot.empty()));

    assertThat(result.intent()).isEqualTo(InterpretationIntent.START_DOCUMENT);
    assertThat(result.documentType()).isEqualTo(InvoiceDocumentType.SALES_RECEIPT);
    assertThat(result.recipientDocumentType()).isEqualTo(IdentityDocumentType.DNI);
    assertThat(result.recipientDocumentNumber()).isEqualTo("12345678");
    assertThat(result.missingFields()).isEmpty();
  }

  @Test
  void interpretsDescriptionFirstItemAndMatchingTotal() {
    var result = provider.interpretText(input(
        "Leche Gloria, dos unidades, precio unitario 3.5 total = 7", receiptContext()));

    assertThat(result.intent()).isEqualTo(InterpretationIntent.ADD_ITEM);
    assertThat(result.items()).singleElement().satisfies(item -> {
      assertThat(item.description()).isEqualTo("Leche gloria");
      assertThat(item.quantity()).isEqualByComparingTo(new BigDecimal("2"));
      assertThat(item.unitPrice()).isEqualByComparingTo(new BigDecimal("3.5"));
    });
    assertThat(result.reportedTotal()).isEqualByComparingTo(new BigDecimal("7"));
    assertThat(result.calculationErrors()).isEmpty();
    assertThat(result.warnings()).isEmpty();
  }

  @Test
  void interpretsQuantityFirstItemWithNumbersInWords() {
    var result = provider.interpretText(input(
        "Agrega tres panes a un sol", receiptContext()));

    assertThat(result.items()).singleElement().satisfies(item -> {
      assertThat(item.description()).isEqualTo("Panes");
      assertThat(item.quantity()).isEqualByComparingTo(new BigDecimal("3"));
      assertThat(item.unitPrice()).isEqualByComparingTo(BigDecimal.ONE);
    });
  }

  @Test
  void reportsMissingItemDataInsteadOfInventingIt() {
    var result = provider.interpretText(input("Agrega Leche Gloria", receiptContext()));

    assertThat(result.intent()).isEqualTo(InterpretationIntent.ADD_ITEM);
    assertThat(result.missingFields())
        .containsExactly("items[0].quantity", "items[0].unitPrice");
    assertThat(result.items().getFirst().quantity()).isNull();
    assertThat(result.items().getFirst().unitPrice()).isNull();
  }

  @Test
  void parsesCompoundAndDecimalSpanishNumbers() {
    assertThat(SpanishNumberParser.parse("treinta y dos").orElseThrow())
        .isEqualByComparingTo(new BigDecimal("32"));
    assertThat(SpanishNumberParser.parse("tres con cincuenta soles").orElseThrow())
        .isEqualByComparingTo(new BigDecimal("3.50"));
  }

  @Test
  void marksConflictingDocumentValuesAsAmbiguous() {
    var result = provider.interpretText(input(
        "Quiero boleta y factura para DNI 12345678 y RUC 20123456789",
        InterpretationContextSnapshot.empty()));

    assertThat(result.ambiguousFields())
        .containsExactly("documentType", "recipientDocument");
    assertThat(result.confidence()).isEqualByComparingTo("0.40");
  }

  private TextInterpretationInput input(
      String text, InterpretationContextSnapshot context) {
    return new TextInterpretationInput("company-1", UUID.randomUUID(), text, context);
  }

  private InterpretationContextSnapshot receiptContext() {
    return new InterpretationContextSnapshot(InvoiceDocumentType.SALES_RECEIPT,
        IdentityDocumentType.DNI, "12345678", "PEN", java.util.List.of());
  }
}
