package com.invoiceautomationservice.infrastructure.adapter.in.web;

import com.invoiceautomationservice.application.dto.response.ElectronicDocumentResponse;
import com.invoiceautomationservice.application.service.ElectronicDocumentService;
import com.invoiceautomationservice.commons.response.ApiResponse;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/electronic-documents")
@RequiredArgsConstructor
public class ElectronicDocumentController {
  private final ElectronicDocumentService service;

  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<ElectronicDocumentResponse>> findById(@PathVariable UUID id) {
    return ResponseEntity.ok(ApiResponse.success(
        200, "Electronic document found", service.findById(id)));
  }
}
