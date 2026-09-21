package com.invoiceautomationservice.infrastructure.adapter.out.persistence;

import static com.invoiceautomationservice.infrastructure.config.exception.RuntimeErrors.ELECTRONIC_DOCUMENT_NOT_FOUND;

import com.invoiceautomationservice.application.port.out.ElectronicDocumentRepository;
import com.invoiceautomationservice.domain.model.ElectronicDocument;
import com.invoiceautomationservice.domain.model.InvoiceItem;
import com.invoiceautomationservice.domain.model.IssuerSnapshot;
import com.invoiceautomationservice.domain.model.RecipientSnapshot;
import com.invoiceautomationservice.infrastructure.adapter.out.persistence.entity.ElectronicDocumentEntity;
import com.invoiceautomationservice.infrastructure.adapter.out.persistence.entity.ElectronicDocumentItemEntity;
import com.invoiceautomationservice.infrastructure.adapter.out.persistence.repository.JpaElectronicDocumentItemRepository;
import com.invoiceautomationservice.infrastructure.adapter.out.persistence.repository.JpaElectronicDocumentRepository;
import com.invoiceautomationservice.infrastructure.config.exception.ApplicationException;
import java.util.List;
import java.util.UUID;
import java.util.Optional;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ElectronicDocumentPersistenceAdapter implements ElectronicDocumentRepository {

  private final JpaElectronicDocumentRepository documentRepository;
  private final JpaElectronicDocumentItemRepository itemRepository;

  @Override
  public ElectronicDocument save(ElectronicDocument document) {
    ElectronicDocumentEntity saved = documentRepository.save(toEntity(document));
    List<InvoiceItem> items = itemRepository.saveAll(IntStream.range(0, document.items().size())
        .mapToObj(index -> toItemEntity(document.items().get(index), document.id(), index))
        .toList()).stream().map(this::toItem).toList();
    return toDomain(saved, items);
  }

  @Override
  public ElectronicDocument findById(UUID id) {
    ElectronicDocumentEntity entity = documentRepository.findById(id)
        .orElseThrow(() -> new ApplicationException(ELECTRONIC_DOCUMENT_NOT_FOUND, id));
    List<InvoiceItem> items = itemRepository.findAllByDocumentIdOrderByPosition(id).stream()
        .map(this::toItem).toList();
    return toDomain(entity, items);
  }

  @Override
  public Optional<ElectronicDocument> findByDraftId(UUID draftId) {
    return documentRepository.findByDraftId(draftId).map(entity -> {
      List<InvoiceItem> items = itemRepository.findAllByDocumentIdOrderByPosition(entity.getId())
          .stream().map(this::toItem).toList();
      return toDomain(entity, items);
    });
  }

  private ElectronicDocumentEntity toEntity(ElectronicDocument document) {
    ElectronicDocumentEntity e = new ElectronicDocumentEntity();
    e.setId(document.id()); e.setDraftId(document.draftId()); e.setCompanyId(document.companyId());
    e.setCustomerId(document.customerId()); e.setDocumentType(document.documentType());
    e.setIssuerTaxId(document.issuer().taxId()); e.setIssuerLegalName(document.issuer().legalName());
    e.setIssuerTradeName(document.issuer().tradeName());
    e.setIssuerTaxpayerType(document.issuer().taxpayerType());
    e.setIssuerFiscalAddress(document.issuer().fiscalAddress());
    e.setIssuerUbigeo(document.issuer().ubigeo());
    e.setIssuerDepartment(document.issuer().department());
    e.setIssuerProvince(document.issuer().province()); e.setIssuerDistrict(document.issuer().district());
    e.setIssuerCountryCode(document.issuer().countryCode());
    e.setRecipientName(document.recipient().name());
    e.setRecipientAddress(document.recipient().address()); e.setRecipientEmail(document.recipient().email());
    e.setSeries(document.series()); e.setCorrelative(document.correlative());
    e.setFullNumber(document.fullNumber()); e.setRecipientDocumentType(document.recipientDocumentType());
    e.setRecipientDocumentNumber(document.recipientDocumentNumber()); e.setCurrency(document.currency());
    e.setSubtotal(document.subtotal()); e.setDiscountTotal(document.discountTotal());
    e.setTaxableTotal(document.taxableTotal()); e.setTaxTotal(document.taxTotal()); e.setTotal(document.total());
    e.setStatus(document.status()); e.setProviderReference(document.providerReference());
    e.setSubmittedAt(document.submittedAt()); e.setRespondedAt(document.respondedAt());
    e.setProviderResponseCode(document.providerResponseCode());
    e.setProviderResponseMessage(document.providerResponseMessage());
    return e;
  }

  private ElectronicDocumentItemEntity toItemEntity(InvoiceItem item, UUID documentId, int position) {
    ElectronicDocumentItemEntity e = new ElectronicDocumentItemEntity();
    e.setId(item.id()); e.setDocumentId(documentId); e.setPosition(position);
    e.setDescription(item.description()); e.setUnitCode(item.unitCode()); e.setQuantity(item.quantity());
    e.setUnitPrice(item.unitPrice()); e.setDiscount(item.discount());
    e.setTaxAffectation(item.taxAffectation()); e.setTaxRate(item.taxRate());
    e.setGrossAmount(item.grossAmount()); e.setTaxableAmount(item.taxableAmount());
    e.setTaxAmount(item.taxAmount()); e.setLineTotal(item.lineTotal());
    return e;
  }

  private InvoiceItem toItem(ElectronicDocumentItemEntity e) {
    return new InvoiceItem(e.getId(), e.getDescription(), e.getUnitCode(), e.getQuantity(),
        e.getUnitPrice(), e.getDiscount(), e.getTaxAffectation(), e.getTaxRate(),
        e.getGrossAmount(), e.getTaxableAmount(), e.getTaxAmount(), e.getLineTotal());
  }

  private ElectronicDocument toDomain(ElectronicDocumentEntity e, List<InvoiceItem> items) {
    IssuerSnapshot issuer = new IssuerSnapshot(
        e.getIssuerTaxId(), e.getIssuerLegalName(), e.getIssuerTradeName(),
        e.getIssuerTaxpayerType(), e.getIssuerFiscalAddress(), e.getIssuerUbigeo(),
        e.getIssuerDepartment(), e.getIssuerProvince(), e.getIssuerDistrict(),
        e.getIssuerCountryCode());
    RecipientSnapshot recipient = new RecipientSnapshot(
        e.getRecipientDocumentType(), e.getRecipientDocumentNumber(), e.getRecipientName(),
        e.getRecipientAddress(), e.getRecipientEmail());
    return new ElectronicDocument(e.getId(), e.getDraftId(), e.getCompanyId(), e.getCustomerId(),
        issuer, recipient, e.getDocumentType(), e.getSeries(), e.getCorrelative(), e.getFullNumber(),
        e.getRecipientDocumentType(), e.getRecipientDocumentNumber(), e.getCurrency(), items,
        e.getSubtotal(), e.getDiscountTotal(), e.getTaxableTotal(), e.getTaxTotal(), e.getTotal(),
        e.getStatus(), e.getProviderReference(), e.getSubmittedAt(), e.getRespondedAt(),
        e.getProviderResponseCode(), e.getProviderResponseMessage());
  }
}
