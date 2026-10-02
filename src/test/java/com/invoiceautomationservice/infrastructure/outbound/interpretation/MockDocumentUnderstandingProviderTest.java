package com.invoiceautomationservice.infrastructure.outbound.interpretation;

import static org.assertj.core.api.Assertions.assertThat;

import com.invoiceautomationservice.domain.model.ImageInterpretationInput;
import com.invoiceautomationservice.domain.model.InterpretationContextSnapshot;
import com.invoiceautomationservice.domain.model.InterpretationIntent;
import com.invoiceautomationservice.domain.model.InterpretationSource;
import com.invoiceautomationservice.domain.model.TextInterpretationInput;
import com.invoiceautomationservice.infrastructure.adapter.out.interpretation.MockDocumentUnderstandingProvider;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MockDocumentUnderstandingProviderTest {
  private final MockDocumentUnderstandingProvider provider =
      new MockDocumentUnderstandingProvider();

  @Test
  void preservesDeterministicNaturalLanguageInterpretationForText() {
    var result = provider.interpretText(new TextInterpretationInput(
        "company-1", UUID.randomUUID(), "Dos leches a 3.50",
        InterpretationContextSnapshot.empty()));

    assertThat(result.source()).isEqualTo(InterpretationSource.TEXT);
    assertThat(result.intent()).isEqualTo(InterpretationIntent.ADD_ITEM);
    assertThat(result.confidence()).isGreaterThan(BigDecimal.ZERO);
    assertThat(result.items()).singleElement().satisfies(item -> {
      assertThat(item.quantity()).isEqualByComparingTo("2");
      assertThat(item.unitPrice()).isEqualByComparingTo("3.50");
    });
  }

  @Test
  void returnsSafeUnknownResultForImage() {
    var result = provider.interpretImage(new ImageInterpretationInput(
        "company-1", UUID.randomUUID(), new byte[] {1}, "image/png", "receipt.png",
        InterpretationContextSnapshot.empty()));

    assertThat(result.source()).isEqualTo(InterpretationSource.IMAGE);
    assertThat(result.intent()).isEqualTo(InterpretationIntent.UNKNOWN);
    assertThat(result.items()).isEmpty();
  }
}
