package com.invoiceautomationservice.application.port.out;

import com.invoiceautomationservice.domain.model.ElectronicDocument;
import java.util.UUID;
import java.util.Optional;
import com.invoiceautomationservice.application.model.PageQuery;
import com.invoiceautomationservice.application.model.PageResult;
import com.invoiceautomationservice.domain.model.ElectronicDocumentStatus;

public interface ElectronicDocumentRepository {
  ElectronicDocument save(ElectronicDocument document);
  ElectronicDocument findById(UUID id);
  ElectronicDocument findByIdForUpdate(UUID id);
  Optional<ElectronicDocument> findByDraftId(UUID draftId);
  PageResult<ElectronicDocument> search(
      String companyId, ElectronicDocumentStatus status, String documentNumber, PageQuery page);
}
