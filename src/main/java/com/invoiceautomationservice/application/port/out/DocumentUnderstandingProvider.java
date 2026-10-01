package com.invoiceautomationservice.application.port.out;

import com.invoiceautomationservice.domain.model.DocumentInterpretation;
import com.invoiceautomationservice.domain.model.ImageInterpretationInput;
import com.invoiceautomationservice.domain.model.TextInterpretationInput;

/**
 * Interprets unstructured billing information without exposing a specific AI provider.
 */
public interface DocumentUnderstandingProvider {

  DocumentInterpretation interpretText(TextInterpretationInput input);

  DocumentInterpretation interpretImage(ImageInterpretationInput input);
}
