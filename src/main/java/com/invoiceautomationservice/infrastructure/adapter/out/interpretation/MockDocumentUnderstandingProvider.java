package com.invoiceautomationservice.infrastructure.adapter.out.interpretation;

import com.invoiceautomationservice.application.port.out.DocumentUnderstandingProvider;
import com.invoiceautomationservice.domain.model.DocumentInterpretation;
import com.invoiceautomationservice.domain.model.ImageInterpretationInput;
import com.invoiceautomationservice.domain.model.InterpretationSource;
import com.invoiceautomationservice.domain.model.TextInterpretationInput;
import java.util.Objects;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Safe default used until a deterministic or multimodal interpreter is configured.
 */
@Component
@ConditionalOnProperty(
    prefix = "ai",
    name = "provider",
    havingValue = "mock",
    matchIfMissing = true)
public class MockDocumentUnderstandingProvider implements DocumentUnderstandingProvider {
  private static final String WARNING =
      "Mock provider does not interpret content; manual review is required";

  @Override
  public DocumentInterpretation interpretText(TextInterpretationInput input) {
    Objects.requireNonNull(input, "input must not be null");
    return DocumentInterpretation.unsupported(InterpretationSource.TEXT, WARNING);
  }

  @Override
  public DocumentInterpretation interpretImage(ImageInterpretationInput input) {
    Objects.requireNonNull(input, "input must not be null");
    return DocumentInterpretation.unsupported(InterpretationSource.IMAGE, WARNING);
  }
}
