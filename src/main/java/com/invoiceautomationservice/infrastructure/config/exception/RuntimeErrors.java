package com.invoiceautomationservice.infrastructure.config.exception;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.GONE;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.PAYLOAD_TOO_LARGE;
import static org.springframework.http.HttpStatus.UNSUPPORTED_MEDIA_TYPE;

@RequiredArgsConstructor
public enum RuntimeErrors implements ApplicationError {

    CUSTOMER_NOT_FOUND(NOT_FOUND, "The customer with id: %s does not exist"),
    COMPANY_NOT_FOUND(NOT_FOUND, "The company with id: %s does not exist"),
    INVOICE_DRAFT_NOT_FOUND(NOT_FOUND, "The invoice draft with id: %s does not exist"),
    COMPANY_INACTIVE(BAD_REQUEST, "The company with id: %s is inactive"),
    CUSTOMER_INACTIVE(BAD_REQUEST, "The customer with id: %s is inactive"),
    COMPANY_CONTEXT_REQUIRED(BAD_REQUEST,
            "X-Company-Id is required when the user does not have exactly one company"),
    COMPANY_PERMISSION_DENIED(FORBIDDEN,
            "Permission %s is required for company %s"),
    USER_IDENTITY_ALREADY_EXISTS(CONFLICT,
            "The username or email is already registered"),
    USER_NOT_FOUND(NOT_FOUND, "The user %s does not exist"),
    COMPANY_MEMBER_NOT_FOUND(NOT_FOUND,
            "The user %s is not a member of company %s"),
    COMPANY_MEMBER_ALREADY_EXISTS(CONFLICT,
            "The user is already a member of company %s"),
    LAST_COMPANY_OWNER(CONFLICT,
            "The company must keep at least one active owner"),
    OWNER_MANAGEMENT_FORBIDDEN(FORBIDDEN,
            "Only an owner can manage owner memberships"),
    CONVERSATION_NOT_FOUND(NOT_FOUND,
            "The conversation with id: %s does not exist"),
    CONVERSATION_RESOURCE_COMPANY_MISMATCH(BAD_REQUEST,
            "The conversation references a resource from another company"),
    IMAGE_EMPTY(BAD_REQUEST, "The image file is empty"),
    IMAGE_TOO_LARGE(PAYLOAD_TOO_LARGE,
            "The image exceeds the maximum allowed size of %s bytes"),
    IMAGE_TYPE_UNSUPPORTED(UNSUPPORTED_MEDIA_TYPE,
            "Only JPEG, PNG and WebP images are supported"),
    IMAGE_CONTENT_TYPE_MISMATCH(BAD_REQUEST,
            "The declared content type %s does not match the detected type %s"),
    IMAGE_SIGNATURE_INVALID(BAD_REQUEST,
            "The file does not contain a valid supported image"),
    IMAGE_DIMENSIONS_INVALID(BAD_REQUEST,
            "Image dimensions %sx%s exceed the allowed dimensions %sx%s or are invalid"),
    IMAGE_RETENTION_INVALID(BAD_REQUEST,
            "Retention days must be between 1 and %s for temporary images"),
    CONVERSATION_IMAGE_NOT_FOUND(NOT_FOUND,
            "The image with id: %s does not exist in conversation %s"),
    CONVERSATION_IMAGE_DELETED(GONE,
            "The image with id: %s is no longer retained"),
    IMAGE_EXTERNAL_MESSAGE_DUPLICATE(CONFLICT,
            "External message id %s already exists in conversation %s"),
    IMAGE_STORAGE_FAILURE(INTERNAL_SERVER_ERROR,
            "The image could not be stored or retrieved"),
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
            "The invoice draft with id: %s already has a numbered electronic document"),
    ELECTRONIC_DOCUMENT_NOT_ADJUSTABLE(CONFLICT,
            "The electronic document with id: %s cannot receive a note from status %s"),
    INVALID_NOTE_REASON(BAD_REQUEST,
            "The reason code %s is invalid for document type %s");

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
