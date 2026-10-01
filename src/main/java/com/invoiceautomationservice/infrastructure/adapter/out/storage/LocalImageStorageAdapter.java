package com.invoiceautomationservice.infrastructure.adapter.out.storage;

import static com.invoiceautomationservice.infrastructure.config.exception.RuntimeErrors.IMAGE_STORAGE_FAILURE;

import com.invoiceautomationservice.application.model.StoredImageObject;
import com.invoiceautomationservice.application.port.out.ImageStoragePort;
import com.invoiceautomationservice.domain.model.ImageFormat;
import com.invoiceautomationservice.infrastructure.config.ImageStorageProperties;
import com.invoiceautomationservice.infrastructure.config.exception.ApplicationException;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "image-storage", name = "provider", havingValue = "local",
    matchIfMissing = true)
public class LocalImageStorageAdapter implements ImageStoragePort {
  private final Path root;

  public LocalImageStorageAdapter(ImageStorageProperties properties) {
    this.root = Path.of(properties.getRoot()).toAbsolutePath().normalize();
  }

  @Override
  public StoredImageObject store(UUID imageId, ImageFormat format, byte[] content) {
    String identifier = imageId.toString();
    String key = identifier.substring(0, 2) + "/" + identifier + "." + format.extension();
    Path target = resolveSafely(key);
    Path temporary = null;
    try {
      Files.createDirectories(target.getParent());
      temporary = Files.createTempFile(target.getParent(), identifier + "-", ".tmp");
      Files.write(temporary, Arrays.copyOf(content, content.length));
      try {
        Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE);
      } catch (AtomicMoveNotSupportedException exception) {
        Files.move(temporary, target);
      }
      return new StoredImageObject(key);
    } catch (IOException exception) {
      deleteQuietly(temporary);
      throw new ApplicationException(IMAGE_STORAGE_FAILURE);
    }
  }

  @Override
  public byte[] load(String key) {
    try {
      return Files.readAllBytes(resolveSafely(key));
    } catch (IOException exception) {
      throw new ApplicationException(IMAGE_STORAGE_FAILURE);
    }
  }

  @Override
  public void delete(String key) {
    try {
      Files.deleteIfExists(resolveSafely(key));
    } catch (IOException exception) {
      throw new ApplicationException(IMAGE_STORAGE_FAILURE);
    }
  }

  private Path resolveSafely(String key) {
    if (key == null || !key.matches("[a-f0-9]{2}/[a-f0-9-]{36}\\.(?:jpg|png|webp)")) {
      throw new ApplicationException(IMAGE_STORAGE_FAILURE);
    }
    Path resolved = root.resolve(key).normalize();
    if (!resolved.startsWith(root)) {
      throw new ApplicationException(IMAGE_STORAGE_FAILURE);
    }
    return resolved;
  }

  private void deleteQuietly(Path path) {
    if (path == null) return;
    try {
      Files.deleteIfExists(path);
    } catch (IOException ignored) {
      // Preserve the original storage failure.
    }
  }
}
