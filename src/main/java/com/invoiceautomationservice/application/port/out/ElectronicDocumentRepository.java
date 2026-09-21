package com.invoiceautomationservice.application.port.out;

import com.invoiceautomationservice.domain.model.ElectronicDocument;
import java.util.UUID;
import java.util.Optional;

public interface ElectronicDocumentRepository {
  ElectronicDocument save(ElectronicDocument document);
  ElectronicDocument findById(UUID id);
  Optional<ElectronicDocument> findByDraftId(UUID draftId);
}
