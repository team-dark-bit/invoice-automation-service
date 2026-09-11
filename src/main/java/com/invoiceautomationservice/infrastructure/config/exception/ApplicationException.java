package com.invoiceautomationservice.infrastructure.config.exception;

import java.util.Objects;
import org.springframework.http.HttpStatus;

public class ApplicationException extends RuntimeException {

  private final ApplicationError error;

  public ApplicationException(ApplicationError error, Object... args) {

    super(Objects.isNull(args) ? error.getMessage() :
            String.format(error.getMessage(), args));
    this.error = error;
  }

  public HttpStatus getStatus() {
    return error.getStatus();
  }

  public String getErrorCode() {
    return error.getCode();
  }

}
