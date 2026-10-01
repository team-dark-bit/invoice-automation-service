package com.invoiceautomationservice.application.service;

import static com.invoiceautomationservice.infrastructure.config.exception.RuntimeErrors.IMAGE_CONTENT_TYPE_MISMATCH;
import static com.invoiceautomationservice.infrastructure.config.exception.RuntimeErrors.IMAGE_DIMENSIONS_INVALID;
import static com.invoiceautomationservice.infrastructure.config.exception.RuntimeErrors.IMAGE_EMPTY;
import static com.invoiceautomationservice.infrastructure.config.exception.RuntimeErrors.IMAGE_SIGNATURE_INVALID;
import static com.invoiceautomationservice.infrastructure.config.exception.RuntimeErrors.IMAGE_TOO_LARGE;
import static com.invoiceautomationservice.infrastructure.config.exception.RuntimeErrors.IMAGE_TYPE_UNSUPPORTED;

import com.invoiceautomationservice.domain.model.ImageFormat;
import com.invoiceautomationservice.infrastructure.config.ImageStorageProperties;
import com.invoiceautomationservice.infrastructure.config.exception.ApplicationException;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import javax.imageio.ImageIO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ImageInspector {
  private final ImageStorageProperties properties;

  public InspectedImage inspect(byte[] content, String declaredContentType) {
    if (content == null || content.length == 0) {
      throw new ApplicationException(IMAGE_EMPTY);
    }
    if (content.length > properties.getMaxSizeBytes()) {
      throw new ApplicationException(IMAGE_TOO_LARGE, properties.getMaxSizeBytes());
    }
    ImageFormat format = detectFormat(content);
    validateDeclaredType(declaredContentType, format);
    int[] dimensions = switch (format) {
      case PNG -> pngDimensions(content);
      case JPEG -> jpegDimensions(content);
      case WEBP -> webpDimensions(content);
    };
    validateDimensions(dimensions[0], dimensions[1]);
    if (format != ImageFormat.WEBP) {
      validateDecodable(content, dimensions);
    }
    return new InspectedImage(format, dimensions[0], dimensions[1], sha256(content));
  }

  private ImageFormat detectFormat(byte[] content) {
    if (startsWith(content, 0xFF, 0xD8, 0xFF)) {
      return ImageFormat.JPEG;
    }
    if (startsWith(content, 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)) {
      return ImageFormat.PNG;
    }
    if (content.length >= 16 && ascii(content, 0, "RIFF") && ascii(content, 8, "WEBP")) {
      return ImageFormat.WEBP;
    }
    throw new ApplicationException(IMAGE_TYPE_UNSUPPORTED);
  }

  private void validateDeclaredType(String declaredContentType, ImageFormat format) {
    if (declaredContentType == null || declaredContentType.isBlank()
        || declaredContentType.equalsIgnoreCase("application/octet-stream")) {
      return;
    }
    String normalized = declaredContentType.split(";", 2)[0].strip().toLowerCase(Locale.ROOT);
    if (!normalized.equals(format.mediaType())
        && !(format == ImageFormat.JPEG && normalized.equals("image/jpg"))) {
      throw new ApplicationException(IMAGE_CONTENT_TYPE_MISMATCH,
          declaredContentType, format.mediaType());
    }
  }

  private int[] pngDimensions(byte[] content) {
    if (content.length < 33 || !ascii(content, 12, "IHDR")) {
      throw new ApplicationException(IMAGE_SIGNATURE_INVALID);
    }
    return new int[]{readIntBigEndian(content, 16), readIntBigEndian(content, 20)};
  }

  private int[] jpegDimensions(byte[] content) {
    if (content.length < 12 || unsigned(content[content.length - 2]) != 0xFF
        || unsigned(content[content.length - 1]) != 0xD9) {
      throw new ApplicationException(IMAGE_SIGNATURE_INVALID);
    }
    int offset = 2;
    while (offset + 3 < content.length) {
      if (unsigned(content[offset]) != 0xFF) {
        offset++;
        continue;
      }
      int marker = unsigned(content[offset + 1]);
      offset += 2;
      if (marker == 0xD8 || marker == 0xD9 || (marker >= 0xD0 && marker <= 0xD7)) {
        continue;
      }
      if (offset + 1 >= content.length) break;
      int length = readUnsignedShort(content, offset);
      if (length < 2 || offset + length > content.length) break;
      if (isStartOfFrame(marker) && length >= 7) {
        return new int[]{readUnsignedShort(content, offset + 5),
            readUnsignedShort(content, offset + 3)};
      }
      offset += length;
    }
    throw new ApplicationException(IMAGE_SIGNATURE_INVALID);
  }

  private int[] webpDimensions(byte[] content) {
    long declaredSize = readUnsignedIntLittleEndian(content, 4) + 8;
    if (declaredSize != content.length || content.length < 26) {
      throw new ApplicationException(IMAGE_SIGNATURE_INVALID);
    }
    int[] canvas = null;
    int[] imageDimensions = null;
    int offset = 12;
    while (offset + 8 <= content.length) {
      String chunk = new String(content, offset, 4,
          java.nio.charset.StandardCharsets.US_ASCII);
      long chunkSize = readUnsignedIntLittleEndian(content, offset + 4);
      long end = (long) offset + 8 + chunkSize;
      if (chunkSize > Integer.MAX_VALUE || end > content.length) {
        throw new ApplicationException(IMAGE_SIGNATURE_INVALID);
      }
      int dataOffset = offset + 8;
      if (chunk.equals("VP8X")) {
        if (chunkSize != 10) throw new ApplicationException(IMAGE_SIGNATURE_INVALID);
        canvas = new int[]{1 + readUnsigned24LittleEndian(content, dataOffset + 4),
            1 + readUnsigned24LittleEndian(content, dataOffset + 7)};
      } else if (chunk.equals("VP8L")) {
        if (chunkSize < 5) throw new ApplicationException(IMAGE_SIGNATURE_INVALID);
        imageDimensions = parseVp8Lossless(content, dataOffset);
      } else if (chunk.equals("VP8 ")) {
        if (chunkSize < 10) throw new ApplicationException(IMAGE_SIGNATURE_INVALID);
        imageDimensions = parseVp8Lossy(content, dataOffset);
      }
      offset = (int) end + (int) (chunkSize & 1L);
    }
    if (offset != content.length || imageDimensions == null) {
      throw new ApplicationException(IMAGE_SIGNATURE_INVALID);
    }
    return canvas == null ? imageDimensions : canvas;
  }

  private int[] parseVp8Lossless(byte[] content, int offset) {
    if (content.length < offset + 5 || unsigned(content[offset]) != 0x2F) {
      throw new ApplicationException(IMAGE_SIGNATURE_INVALID);
    }
    int width = 1 + unsigned(content[offset + 1])
        + ((unsigned(content[offset + 2]) & 0x3F) << 8);
    int height = 1 + ((unsigned(content[offset + 2]) & 0xC0) >> 6)
        + (unsigned(content[offset + 3]) << 2)
        + ((unsigned(content[offset + 4]) & 0x0F) << 10);
    return new int[]{width, height};
  }

  private int[] parseVp8Lossy(byte[] content, int offset) {
    if (content.length < offset + 10 || !startsWithAt(content, offset + 3, 0x9D, 0x01, 0x2A)) {
      throw new ApplicationException(IMAGE_SIGNATURE_INVALID);
    }
    int width = (unsigned(content[offset + 6]) | unsigned(content[offset + 7]) << 8) & 0x3FFF;
    int height = (unsigned(content[offset + 8]) | unsigned(content[offset + 9]) << 8) & 0x3FFF;
    return new int[]{width, height};
  }

  private void validateDimensions(int width, int height) {
    if (width <= 0 || height <= 0 || width > properties.getMaxWidth()
        || height > properties.getMaxHeight()) {
      throw new ApplicationException(IMAGE_DIMENSIONS_INVALID, width, height,
          properties.getMaxWidth(), properties.getMaxHeight());
    }
  }

  private void validateDecodable(byte[] content, int[] dimensions) {
    try {
      BufferedImage image = ImageIO.read(new ByteArrayInputStream(content));
      if (image == null || image.getWidth() != dimensions[0] || image.getHeight() != dimensions[1]) {
        throw new ApplicationException(IMAGE_SIGNATURE_INVALID);
      }
    } catch (IOException exception) {
      throw new ApplicationException(IMAGE_SIGNATURE_INVALID);
    }
  }

  private String sha256(byte[] content) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 is not available", exception);
    }
  }

  private boolean isStartOfFrame(int marker) {
    return marker >= 0xC0 && marker <= 0xCF
        && marker != 0xC4 && marker != 0xC8 && marker != 0xCC;
  }

  private boolean startsWith(byte[] content, int... expected) {
    return startsWithAt(content, 0, expected);
  }

  private boolean startsWithAt(byte[] content, int offset, int... expected) {
    if (content.length < offset + expected.length) return false;
    for (int index = 0; index < expected.length; index++) {
      if (unsigned(content[offset + index]) != expected[index]) return false;
    }
    return true;
  }

  private boolean ascii(byte[] content, int offset, String expected) {
    return offset >= 0 && content.length >= offset + expected.length()
        && new String(content, offset, expected.length(), java.nio.charset.StandardCharsets.US_ASCII)
        .equals(expected);
  }

  private int readIntBigEndian(byte[] value, int offset) {
    if (value.length < offset + 4) throw new ApplicationException(IMAGE_SIGNATURE_INVALID);
    return unsigned(value[offset]) << 24 | unsigned(value[offset + 1]) << 16
        | unsigned(value[offset + 2]) << 8 | unsigned(value[offset + 3]);
  }

  private int readUnsignedShort(byte[] value, int offset) {
    return unsigned(value[offset]) << 8 | unsigned(value[offset + 1]);
  }

  private long readUnsignedIntLittleEndian(byte[] value, int offset) {
    if (value.length < offset + 4) throw new ApplicationException(IMAGE_SIGNATURE_INVALID);
    return (long) unsigned(value[offset]) | (long) unsigned(value[offset + 1]) << 8
        | (long) unsigned(value[offset + 2]) << 16 | (long) unsigned(value[offset + 3]) << 24;
  }

  private int readUnsigned24LittleEndian(byte[] value, int offset) {
    if (value.length < offset + 3) throw new ApplicationException(IMAGE_SIGNATURE_INVALID);
    return unsigned(value[offset]) | unsigned(value[offset + 1]) << 8
        | unsigned(value[offset + 2]) << 16;
  }

  private int unsigned(byte value) {
    return value & 0xFF;
  }
}
