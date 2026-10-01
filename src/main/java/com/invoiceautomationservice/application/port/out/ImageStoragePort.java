package com.invoiceautomationservice.application.port.out;

import com.invoiceautomationservice.application.model.StoredImageObject;
import com.invoiceautomationservice.domain.model.ImageFormat;
import java.util.UUID;

public interface ImageStoragePort {
  StoredImageObject store(UUID imageId, ImageFormat format, byte[] content);
  byte[] load(String key);
  void delete(String key);
}
