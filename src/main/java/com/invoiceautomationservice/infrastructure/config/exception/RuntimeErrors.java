package com.invoiceautomationservice.infrastructure.config.exception;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.CONFLICT;

@RequiredArgsConstructor
public enum RuntimeErrors implements ApplicationError {

    CUSTOMER_NOT_FOUND(NOT_FOUND, "The customer with id: %s does not exist"),
    COMPANY_NOT_FOUND(NOT_FOUND, "The company with id: %s does not exist"),
    INVOICE_DRAFT_NOT_FOUND(NOT_FOUND, "The invoice draft with id: %s does not exist"),
    COMPANY_INACTIVE(BAD_REQUEST, "The company with id: %s is inactive"),
    CUSTOMER_INACTIVE(BAD_REQUEST, "The customer with id: %s is inactive"),
    COMPANY_CONTEXT_REQUIRED(BAD_REQUEST,
            "X-Company-Id is required when the user does not have exactly one company"),
    ISSUER_TAX_PROFILE_NOT_FOUND(NOT_FOUND,
            "The issuer tax profile for company: %s does not exist"),
    INVALID_ISSUER_RUC(BAD_REQUEST,
            "The company with id: %s must have a valid 11-digit RUC"),
    ISSUER_ONBOARDING_REQUIRED(BAD_REQUEST,
            "The issuer tax onboarding for company: %s must be completed before creating drafts"),
    INVALID_RECIPIENT_DOCUMENT(BAD_REQUEST,
            "The %s recipient document must contain %s digits"),
    INVOICE_REQUIRES_RUC(BAD_REQUEST,
            "An invoice recipient must be identified with RUC"),
    INVALID_DOCUMENT_SERIES(BAD_REQUEST,
            "The series %s is invalid for document type %s"),
    DOCUMENT_SERIES_NOT_FOUND(NOT_FOUND,
            "No active series exists for document type %s and company %s"),
    ELECTRONIC_DOCUMENT_NOT_FOUND(NOT_FOUND,
            "The electronic document with id: %s does not exist"),
    ELECTRONIC_DOCUMENT_NOT_RETRYABLE(CONFLICT,
            "The electronic document with id: %s cannot be retried from status %s"),
    INVOICE_DRAFT_ALREADY_NUMBERED(CONFLICT,
            "The invoice draft with id: %s already has a numbered electronic document");

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
