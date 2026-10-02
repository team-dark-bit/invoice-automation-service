package com.invoiceautomationservice.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.invoiceautomationservice.application.model.ImageProcessingWork;
import com.invoiceautomationservice.application.port.out.DocumentUnderstandingProvider;
import com.invoiceautomationservice.application.port.out.ImageStoragePort;
import com.invoiceautomationservice.domain.model.ConversationImage;
import com.invoiceautomationservice.domain.model.DocumentInterpretation;
import com.invoiceautomationservice.domain.model.ImageFormat;
import com.invoiceautomationservice.domain.model.ImageInterpretationInput;
import com.invoiceautomationservice.domain.model.ImageProcessingStatus;
import com.invoiceautomationservice.domain.model.ImageRetentionPolicy;
import com.invoiceautomationservice.domain.model.InterpretationContextSnapshot;
import com.invoiceautomationservice.domain.model.InterpretationSource;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ConversationImageProcessorTest {
  private ImageProcessingStateService stateService;
  private ImageStoragePort storage;
  private DocumentUnderstandingProvider provider;
  private ConversationImageProcessor processor;
  private ConversationImage processing;

  @BeforeEach
  void setUp() {
    stateService = mock(ImageProcessingStateService.class);
    storage = mock(ImageStoragePort.class);
    provider = mock(DocumentUnderstandingProvider.class);
    processor = new ConversationImageProcessor(stateService, storage, provider);
    processing = processingImage();
    when(stateService.start(processing.id())).thenReturn(Optional.of(
        new ImageProcessingWork(processing, "company-1", InterpretationContextSnapshot.empty())));
    when(storage.load(processing.storageKey())).thenReturn(new byte[] {1, 2, 3});
  }

  @Test
  void interpretsStoredImageThroughUnifiedProviderAndCompletesProcessing() {
    DocumentInterpretation interpretation = DocumentInterpretation.unsupported(
        InterpretationSource.IMAGE, "manual review is required");
    when(provider.interpretImage(any())).thenReturn(interpretation);

    processor.process(processing.id());

    ArgumentCaptor<ImageInterpretationInput> input =
        ArgumentCaptor.forClass(ImageInterpretationInput.class);
    verify(provider).interpretImage(input.capture());
    assertThat(input.getValue().companyId()).isEqualTo("company-1");
    assertThat(input.getValue().mimeType()).isEqualTo("image/png");
    assertThat(input.getValue().content()).containsExactly(1, 2, 3);
    verify(stateService).complete(processing.id(), interpretation);
  }

  @Test
  void marksProcessingAsFailedWhenProviderThrows() {
    when(provider.interpretImage(any())).thenThrow(new IllegalStateException("secret detail"));

    processor.process(processing.id());

    verify(stateService).fail(processing.id(),
        "Multimodal processing failed: IllegalStateException");
  }

  private ConversationImage processingImage() {
    Instant createdAt = Instant.parse("2026-10-02T10:00:00Z");
    return new ConversationImage(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
        "2026/10/image.png", "receipt.png", ImageFormat.PNG, 128, 10, 20, "a".repeat(64),
        ImageRetentionPolicy.TEMPORARY, createdAt.plusSeconds(3600), createdAt, null,
        ImageProcessingStatus.PROCESSING, 1, createdAt.plusSeconds(1), null, null, null);
  }
}
