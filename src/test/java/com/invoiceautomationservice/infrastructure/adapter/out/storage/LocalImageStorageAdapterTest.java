package com.invoiceautomationservice.infrastructure.adapter.out.storage;

import static org.assertj.core.api.Assertions.assertThat;

import com.invoiceautomationservice.domain.model.ImageFormat;
import com.invoiceautomationservice.infrastructure.config.ImageStorageProperties;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LocalImageStorageAdapterTest {
  @TempDir Path directory;

  @Test
  void storesWithGeneratedSafeNameAndCanLoadAndDelete() {
    ImageStorageProperties properties = new ImageStorageProperties();
    properties.setRoot(directory.toString());
    LocalImageStorageAdapter adapter = new LocalImageStorageAdapter(properties);
    byte[] content = {1, 2, 3};

    var stored = adapter.store(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"),
        ImageFormat.PNG, content);

    assertThat(stored.key()).isEqualTo("12/123e4567-e89b-12d3-a456-426614174000.png");
    assertThat(adapter.load(stored.key())).containsExactly(content);
    adapter.delete(stored.key());
    assertThat(Files.exists(directory.resolve(stored.key()))).isFalse();
  }
}
