package com.invoiceautomationservice.application.service;

import com.invoiceautomationservice.application.port.out.BillingProvider;
import com.invoiceautomationservice.domain.model.BillingResult;
import com.invoiceautomationservice.domain.model.BillingSubmission;
import com.invoiceautomationservice.domain.model.ElectronicDocument;
import com.invoiceautomationservice.domain.model.ElectronicDocumentStatus;
import com.invoiceautomationservice.domain.model.IssuerSnapshot;
import com.invoiceautomationservice.domain.model.RecipientSnapshot;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BillingSubmissionService {

  private final BillingProvider billingProvider;
  private final InvoiceIssuancePersistenceService persistenceService;
  private final Clock clock;

  public ElectronicDocument submit(ElectronicDocument document) {
    BillingSubmission submission = toSubmission(document);
    BillingResult result;
    try {
      result = billingProvider.submit(submission);
    } catch (RuntimeException exception) {
      Instant failedAt = Instant.now(clock);
      result = new BillingResult(
          null, ElectronicDocumentStatus.ERROR, failedAt, failedAt,
          "PROVIDER_CALL_FAILED", providerFailureMessage(exception));
    }
    return persistenceService.complete(document.id(), document.submittedAt(), result);
  }

  BillingSubmission toSubmission(ElectronicDocument document) {
    IssuerSnapshot snapshot = document.issuer();
    BillingSubmission.Issuer issuer = new BillingSubmission.Issuer(
        snapshot.taxId(), snapshot.legalName(), snapshot.tradeName(), snapshot.taxpayerType(),
        snapshot.fiscalAddress(), snapshot.ubigeo(), snapshot.department(), snapshot.province(),
        snapshot.district(), snapshot.countryCode());
    RecipientSnapshot recipientSnapshot = document.recipient();
    BillingSubmission.Recipient recipient = new BillingSubmission.Recipient(
        recipientSnapshot.documentType(), recipientSnapshot.documentNumber(),
        recipientSnapshot.name(), recipientSnapshot.address(), recipientSnapshot.email());
    List<BillingSubmission.Item> providerItems = document.items().stream()
        .map(item -> new BillingSubmission.Item(
            item.description(), item.unitCode(), item.quantity(), item.unitPrice(), item.discount(),
            item.taxAffectation(), item.taxRate(), item.grossAmount(), item.taxableAmount(),
            item.taxAmount(), item.lineTotal()))
        .toList();
    return new BillingSubmission(
        document.id(), document.id().toString(), document.fullNumber(),
        document.series(), document.correlative(), document.documentType(), document.currency(),
        issuer, recipient, providerItems, document.subtotal(), document.discountTotal(),
        document.taxableTotal(), document.taxTotal(), document.total());
  }

  private String providerFailureMessage(RuntimeException exception) {
    String message = exception.getMessage();
    String detail = message == null || message.isBlank()
        ? exception.getClass().getSimpleName() : message;
    return detail.length() <= 1000 ? detail : detail.substring(0, 1000);
  }
}
