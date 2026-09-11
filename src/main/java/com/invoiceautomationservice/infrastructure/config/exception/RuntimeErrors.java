package com.invoiceautomationservice.infrastructure.config.exception;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@RequiredArgsConstructor
public enum RuntimeErrors implements ApplicationError {

    CUSTOMER_NOT_FOUND(NOT_FOUND, "The customer with id: %s does not exist");

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
