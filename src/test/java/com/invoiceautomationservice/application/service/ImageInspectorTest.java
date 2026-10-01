package com.invoiceautomationservice.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.invoiceautomationservice.domain.model.ImageFormat;
import com.invoiceautomationservice.infrastructure.config.ImageStorageProperties;
import com.invoiceautomationservice.infrastructure.config.exception.ApplicationException;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ImageInspectorTest {
  private ImageStorageProperties properties;
  private ImageInspector inspector;

  @BeforeEach
  void setUp() {
    properties = new ImageStorageProperties();
    inspector = new ImageInspector(properties);
  }

  @Test
  void detectsAndDecodesPngUsingItsRealSignature() throws Exception {
    var result = inspector.inspect(image("png", 4, 3), "image/png");

    assertThat(result.format()).isEqualTo(ImageFormat.PNG);
    assertThat(result.width()).isEqualTo(4);
    assertThat(result.height()).isEqualTo(3);
    assertThat(result.sha256()).matches("[a-f0-9]{64}");
  }

  @Test
  void detectsAndDecodesJpegUsingItsRealSignature() throws Exception {
    var result = inspector.inspect(image("jpg", 5, 2), "image/jpeg");

    assertThat(result.format()).isEqualTo(ImageFormat.JPEG);
    assertThat(result.width()).isEqualTo(5);
    assertThat(result.height()).isEqualTo(2);
  }

  @Test
  void readsWebpDimensionsWithoutTrustingTheFilename() {
    var result = inspector.inspect(webpVp8x(7, 9), "image/webp");

    assertThat(result.format()).isEqualTo(ImageFormat.WEBP);
    assertThat(result.width()).isEqualTo(7);
    assertThat(result.height()).isEqualTo(9);
  }

  @Test
  void rejectsContentTypeThatDoesNotMatchTheSignature() throws Exception {
    assertThatThrownBy(() -> inspector.inspect(image("png", 2, 2), "image/jpeg"))
        .isInstanceOf(ApplicationException.class)
        .hasMessageContaining("does not match");
  }

  @Test
  void rejectsInvalidOrTruncatedImage() {
    assertThatThrownBy(() -> inspector.inspect(
        new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}, "image/jpeg"))
        .isInstanceOf(ApplicationException.class)
        .hasMessageContaining("valid supported image");
  }

  @Test
  void enforcesConfiguredDimensionLimitsBeforeDecoding() throws Exception {
    properties.setMaxWidth(1);

    assertThatThrownBy(() -> inspector.inspect(image("png", 2, 1), "image/png"))
        .isInstanceOf(ApplicationException.class)
        .hasMessageContaining("dimensions");
  }

  @Test
  void enforcesConfiguredFileSizeLimit() throws Exception {
    byte[] content = image("png", 2, 2);
    properties.setMaxSizeBytes(content.length - 1L);

    assertThatThrownBy(() -> inspector.inspect(content, "image/png"))
        .isInstanceOf(ApplicationException.class)
        .hasMessageContaining("maximum allowed size");
  }

  private byte[] image(String format, int width, int height) throws Exception {
    BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    ImageIO.write(image, format, output);
    return output.toByteArray();
  }

  private byte[] webpVp8x(int width, int height) {
    byte[] value = new byte[44];
    ascii(value, 0, "RIFF");
    writeLittleEndian(value, 4, value.length - 8, 4);
    ascii(value, 8, "WEBP");
    ascii(value, 12, "VP8X");
    writeLittleEndian(value, 16, 10, 4);
    writeLittleEndian(value, 24, width - 1, 3);
    writeLittleEndian(value, 27, height - 1, 3);
    ascii(value, 30, "VP8L");
    writeLittleEndian(value, 34, 5, 4);
    value[38] = 0x2F;
    int encodedWidth = width - 1;
    int encodedHeight = height - 1;
    value[39] = (byte) encodedWidth;
    value[40] = (byte) ((encodedWidth >> 8) | (encodedHeight & 0x03) << 6);
    value[41] = (byte) (encodedHeight >> 2);
    value[42] = (byte) (encodedHeight >> 10);
    return value;
  }

  private void ascii(byte[] target, int offset, String value) {
    byte[] bytes = value.getBytes(java.nio.charset.StandardCharsets.US_ASCII);
    System.arraycopy(bytes, 0, target, offset, bytes.length);
  }

  private void writeLittleEndian(byte[] target, int offset, int value, int bytes) {
    for (int index = 0; index < bytes; index++) {
      target[offset + index] = (byte) (value >> (8 * index));
    }
  }
}
