package com.invoiceautomationservice.domain.model;

public enum ImageFormat {
  JPEG("image/jpeg", "jpg"),
  PNG("image/png", "png"),
  WEBP("image/webp", "webp");

  private final String mediaType;
  private final String extension;

  ImageFormat(String mediaType, String extension) {
    this.mediaType = mediaType;
    this.extension = extension;
  }

  public String mediaType() {
    return mediaType;
  }

  public String extension() {
    return extension;
  }
}
