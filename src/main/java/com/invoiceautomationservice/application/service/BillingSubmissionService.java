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
import java.math.BigDecimal;
import java.math.RoundingMode;
import com.invoiceautomationservice.domain.model.TaxAffectation;
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
            item.id().toString(), item.description(), item.unitCode(), item.quantity(),
            item.unitPrice(), item.discount(),
            item.taxAffectation(), item.taxRate(), item.grossAmount(), item.taxableAmount(),
            item.taxAmount(), item.lineTotal(), item.unitPrice(),
            item.unitPrice().multiply(
                BigDecimal.ONE.add(item.taxRate().divide(new BigDecimal("100"))))
                .setScale(2, RoundingMode.HALF_UP),
            item.grossAmount().subtract(item.discount()),
            item.taxAffectation().nubefactCode()))
        .toList();
    BigDecimal exemptTotal = totalByAffectation(document, TaxAffectation.EXEMPT);
    BigDecimal unaffectedTotal = totalByAffectation(document, TaxAffectation.UNAFFECTED);
    return new BillingSubmission(
        document.id(), document.id().toString(), document.fullNumber(),
        document.series(), document.correlative(), document.documentType(),
        "generar_comprobante", "1", document.emissionAt(), document.currency(),
        currencyCode(document.currency()),
        issuer, recipient, providerItems, document.subtotal(), document.discountTotal(),
        document.taxableTotal(), exemptTotal, unaffectedTotal, document.taxTotal(),
        document.total(), new BigDecimal("18.00"), true, false);
  }

  private BigDecimal totalByAffectation(
      ElectronicDocument document, TaxAffectation affectation) {
    return document.items().stream()
        .filter(item -> item.taxAffectation() == affectation)
        .map(item -> item.grossAmount().subtract(item.discount()))
        .reduce(BigDecimal.ZERO.setScale(2), BigDecimal::add);
  }

  private String currencyCode(String currency) {
    return switch (currency) {
      case "PEN" -> "1";
      case "USD" -> "2";
      default -> throw new IllegalArgumentException(
          "NUBEFACT only supports PEN and USD in the minimum contract");
    };
  }

  private String providerFailureMessage(RuntimeException exception) {
    String message = exception.getMessage();
    String detail = message == null || message.isBlank()
        ? exception.getClass().getSimpleName() : message;
    return detail.length() <= 1000 ? detail : detail.substring(0, 1000);
  }
}
