package com.invoiceautomationservice.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class InterpretationInputTest {

  @Test
  void imageInputDefensivelyCopiesBinaryContent() {
    byte[] original = {1, 2, 3};
    ImageInterpretationInput input = new ImageInterpretationInput(
        " company-1 ", UUID.randomUUID(), original, " IMAGE/JPEG ",
        " receipt.jpg ", InterpretationContextSnapshot.empty());

    original[0] = 9;
    byte[] returned = input.content();
    returned[1] = 9;

    assertThat(input.companyId()).isEqualTo("company-1");
    assertThat(input.mimeType()).isEqualTo("image/jpeg");
    assertThat(input.fileName()).isEqualTo("receipt.jpg");
    assertThat(input.content()).containsExactly(1, 2, 3);
  }

  @Test
  void rejectsEmptyImageAndBlankText() {
    UUID conversationId = UUID.randomUUID();

    assertThatThrownBy(() -> new ImageInterpretationInput(
        "company-1", conversationId, new byte[0], "image/jpeg", "receipt.jpg",
        InterpretationContextSnapshot.empty()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("content");
    assertThatThrownBy(() -> new TextInterpretationInput(
        "company-1", conversationId, " ", InterpretationContextSnapshot.empty()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("text");
  }
}
