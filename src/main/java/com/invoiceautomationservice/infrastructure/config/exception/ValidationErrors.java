package com.invoiceautomationservice.infrastructure.config.exception;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

@RequiredArgsConstructor
public enum ValidationErrors implements ApplicationError {

  DECIMAL_BAD_REQUEST(BAD_REQUEST, "Invalid decimal format"),
  FIELD_REQUIRED(BAD_REQUEST, "The field %s must not be null or empty");


  private final HttpStatus status;
  private final String message;

  @Override
  public HttpStatus getStatus() {
    return this.status;
  }

  @Override
  public String getCode() {
    return this.name();
  }

  @Override
  public String getMessage() {
    return this.message;
  }
}
