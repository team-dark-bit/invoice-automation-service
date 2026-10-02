package com.invoiceautomationservice.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ConversationImageProcessingTest {
  private static final Instant CREATED_AT = Instant.parse("2026-10-02T10:00:00Z");

  @Test
  void transitionsFromReceivedToExtractedAndKeepsInterpretation() {
    ConversationImage received = received();
    DocumentInterpretation interpretation = DocumentInterpretation.unsupported(
        InterpretationSource.IMAGE, "manual review is required");

    ConversationImage processing = received.startProcessing(CREATED_AT.plusSeconds(1));
    ConversationImage extracted = processing.extracted(
        interpretation, CREATED_AT.plusSeconds(2));

    assertThat(processing.processingStatus()).isEqualTo(ImageProcessingStatus.PROCESSING);
    assertThat(processing.processingAttempts()).isEqualTo(1);
    assertThat(extracted.processingStatus()).isEqualTo(ImageProcessingStatus.EXTRACTED);
    assertThat(extracted.interpretation()).isEqualTo(interpretation);
    assertThat(extracted.processedAt()).isEqualTo(CREATED_AT.plusSeconds(2));
  }

  @Test
  void failedProcessingCanBeQueuedAgainWithoutLosingAttemptCount() {
    ConversationImage failed = received().startProcessing(CREATED_AT.plusSeconds(1))
        .failProcessing("provider unavailable", CREATED_AT.plusSeconds(2));

    ConversationImage retried = failed.retry();
    ConversationImage secondAttempt = retried.startProcessing(CREATED_AT.plusSeconds(3));

    assertThat(retried.processingStatus()).isEqualTo(ImageProcessingStatus.RECEIVED);
    assertThat(retried.processingAttempts()).isEqualTo(1);
    assertThat(secondAttempt.processingAttempts()).isEqualTo(2);
    assertThat(secondAttempt.lastProcessingError()).isNull();
  }

  @Test
  void extractedImageCannotBeRetried() {
    ConversationImage extracted = received().startProcessing(CREATED_AT.plusSeconds(1))
        .extracted(DocumentInterpretation.unsupported(
            InterpretationSource.IMAGE, "manual review is required"),
            CREATED_AT.plusSeconds(2));

    assertThatThrownBy(extracted::retry)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("only failed");
  }

  private ConversationImage received() {
    return new ConversationImage(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
        "2026/10/image.png", "receipt.png", ImageFormat.PNG, 128, 10, 20, "a".repeat(64),
        ImageRetentionPolicy.TEMPORARY, CREATED_AT.plusSeconds(3600), CREATED_AT, null,
        ImageProcessingStatus.RECEIVED, 0, null, null, null, null);
  }
}
