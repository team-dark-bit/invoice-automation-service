package com.invoiceautomationservice.infrastructure.config.exception;

import com.invoiceautomationservice.commons.response.ApiResponse;
import com.invoiceautomationservice.domain.exception.InvalidInvoiceDraftStateException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApplicationException.class)
    public ResponseEntity<ApiResponse<Void>> handleCustomException(ApplicationException ex) {
      List<String> errors = List.of(ex.getMessage());
      ApiResponse<Void> response = ApiResponse.failure(ex.getStatus().value(), "A problem occurred", errors);
      return new ResponseEntity<>(response, ex.getStatus());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException ex) {
      List<String> errors = ex.getBindingResult().getFieldErrors().stream()
              .map(error -> error.getDefaultMessage())
              .distinct()
              .toList();
      ApiResponse<Void> response = ApiResponse.failure(
              HttpStatus.BAD_REQUEST.value(), "Validation failed", errors
      );
      return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(InvalidInvoiceDraftStateException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidDraftState(InvalidInvoiceDraftStateException ex) {
      ApiResponse<Void> response = ApiResponse.failure(
              HttpStatus.CONFLICT.value(), "Invalid invoice draft state", List.of(ex.getMessage())
      );
      return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

}
