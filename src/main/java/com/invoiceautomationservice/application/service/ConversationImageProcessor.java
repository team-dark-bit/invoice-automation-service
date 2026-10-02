package com.invoiceautomationservice.application.service;

import com.invoiceautomationservice.application.port.out.DocumentUnderstandingProvider;
import com.invoiceautomationservice.application.port.out.ImageStoragePort;
import com.invoiceautomationservice.domain.model.ImageInterpretationInput;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationImageProcessor {
  private final ImageProcessingStateService stateService;
  private final ImageStoragePort storage;
  private final DocumentUnderstandingProvider understandingProvider;

  public void process(UUID imageId) {
    var work = stateService.start(imageId);
    if (work.isEmpty()) return;
    try {
      var value = work.get();
      byte[] content = storage.load(value.image().storageKey());
      var interpretation = understandingProvider.interpretImage(new ImageInterpretationInput(
          value.companyId(), value.image().conversationId(), content,
          value.image().format().mediaType(), value.image().originalFilename(), value.context()));
      stateService.complete(imageId, interpretation);
    } catch (RuntimeException exception) {
      log.error("Multimodal processing failed for image {}", imageId, exception);
      stateService.fail(imageId,
          "Multimodal processing failed: " + exception.getClass().getSimpleName());
    }
  }
}
