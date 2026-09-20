package com.invoiceautomationservice.application.service;

import com.invoiceautomationservice.application.dto.response.ElectronicDocumentResponse;
import com.invoiceautomationservice.application.dto.response.InvoiceItemResponse;
import com.invoiceautomationservice.application.port.out.ElectronicDocumentRepository;
import com.invoiceautomationservice.domain.model.ElectronicDocument;
import com.invoiceautomationservice.domain.model.InvoiceItem;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ElectronicDocumentService {

  private final ElectronicDocumentRepository repository;
  private final CompanyAccessService companyAccessService;

  @Transactional(readOnly = true)
  public ElectronicDocumentResponse findById(UUID id) {
    ElectronicDocument document = repository.findById(id);
    companyAccessService.requireAccess(document.companyId());
    return toResponse(document);
  }

  public ElectronicDocumentResponse toResponse(ElectronicDocument document) {
    return new ElectronicDocumentResponse(
        document.id(), document.draftId(), document.companyId(), document.customerId(),
        document.documentType(), document.series(), document.correlative(), document.fullNumber(),
        document.recipientDocumentType(), document.recipientDocumentNumber(), document.currency(),
        document.items().stream().map(this::toItemResponse).toList(), document.subtotal(),
        document.discountTotal(), document.taxableTotal(), document.taxTotal(), document.total(),
        document.providerReference(), document.issuedAt());
  }

  private InvoiceItemResponse toItemResponse(InvoiceItem item) {
    return new InvoiceItemResponse(
        item.id(), item.description(), item.unitCode(), item.quantity(), item.unitPrice(),
        item.discount(), item.taxAffectation(), item.taxRate(), item.grossAmount(),
        item.taxableAmount(), item.taxAmount(), item.lineTotal());
  }
}
