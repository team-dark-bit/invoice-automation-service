package com.invoiceautomationservice.application.port.out;

import com.invoiceautomationservice.domain.model.ElectronicDocument;
import java.util.UUID;

public interface ElectronicDocumentRepository {
  ElectronicDocument save(ElectronicDocument document);
  ElectronicDocument findById(UUID id);
}
