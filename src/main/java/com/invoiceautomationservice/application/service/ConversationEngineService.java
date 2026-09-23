package com.invoiceautomationservice.application.service;

import com.invoiceautomationservice.application.dto.request.CreateInvoiceDraftRequest;
import com.invoiceautomationservice.application.dto.request.CreateInvoiceItemRequest;
import com.invoiceautomationservice.application.dto.response.ConversationContextResponse;
import com.invoiceautomationservice.application.dto.response.ConversationEngineResponse;
import com.invoiceautomationservice.application.port.in.ConversationEngineUseCase;
import com.invoiceautomationservice.application.port.in.InvoiceDraftUseCase;
import com.invoiceautomationservice.application.port.out.ConversationContextRepository;
import com.invoiceautomationservice.application.port.out.ConversationRepository;
import com.invoiceautomationservice.application.port.out.MessageRepository;
import com.invoiceautomationservice.domain.model.AuditAction;
import com.invoiceautomationservice.domain.model.CompanyPermission;
import com.invoiceautomationservice.domain.model.ConversationContext;
import com.invoiceautomationservice.domain.model.ConversationDraftItem;
import com.invoiceautomationservice.domain.model.IdentityDocumentType;
import com.invoiceautomationservice.domain.model.InvoiceDocumentType;
import com.invoiceautomationservice.domain.model.Message;
import com.invoiceautomationservice.domain.model.MessageDirection;
import com.invoiceautomationservice.domain.model.MessageStatus;
import com.invoiceautomationservice.domain.model.MessageType;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ConversationEngineService implements ConversationEngineUseCase {
  private static final String HELP = "Comandos: NUEVA BOLETA DNI 12345678 PEN; "
      + "NUEVA FACTURA RUC 20123456789 PEN; AGREGAR cantidad | descripción | precio; "
      + "RESUMEN; GENERAR; CANCELAR.";

  private final ConversationRepository conversationRepository;
  private final ConversationContextRepository contextRepository;
  private final MessageRepository messageRepository;
  private final InvoiceDraftUseCase invoiceDraftUseCase;
  private final CompanyAccessService accessService;
  private final ConversationCommandParser parser;
  private final AuditTrailService auditTrailService;
  private final Clock clock;

  @Override
  @Transactional
  public ConversationEngineResponse process(
      UUID conversationId, String text, String externalMessageId) {
    var conversation = conversationRepository.findByIdForUpdate(conversationId);
    accessService.requirePermission(conversation.companyId(), CompanyPermission.CONVERSATION_MANAGE);
    if (externalMessageId != null && !externalMessageId.isBlank()
        && messageRepository.existsByConversationIdAndExternalMessageId(
            conversationId, externalMessageId.strip())) {
      ConversationContext existing = contextRepository.findByConversationId(conversationId)
          .orElseGet(() -> ConversationContext.empty(conversationId, Instant.now(clock)));
      return new ConversationEngineResponse(conversationId, "Mensaje ya procesado.",
          toResponse(existing));
    }
    conversation.ensureOpen();
    Instant now = Instant.now(clock);
    messageRepository.save(Message.create(conversationId, MessageDirection.INBOUND,
        MessageType.TEXT, text, null, externalMessageId, MessageStatus.RECEIVED, now));
    ConversationContext context = contextRepository.findByConversationId(conversationId)
        .orElseGet(() -> ConversationContext.empty(conversationId, now));
    ProcessingResult result = execute(conversation.companyId(), context, parser.parse(text), now);
    ConversationContext savedContext = contextRepository.save(result.context());
    messageRepository.save(Message.create(conversationId, MessageDirection.OUTBOUND,
        MessageType.TEXT, result.reply(), null, null, MessageStatus.SENT, now));
    conversationRepository.save(conversation.touch(now));
    auditTrailService.record(conversation.companyId(), AuditAction.CONVERSATION_PROCESSED,
        "CONVERSATION", conversationId, "SUCCESS", result.commandName());
    return new ConversationEngineResponse(conversationId, result.reply(), toResponse(savedContext));
  }

  private ProcessingResult execute(String companyId, ConversationContext context,
      ConversationCommandParser.Command command, Instant now) {
    if (command instanceof ConversationCommandParser.HelpCommand) {
      return result(context, HELP, "HELP");
    }
    if (command instanceof ConversationCommandParser.StartCommand start) {
      if (start.documentType() == InvoiceDocumentType.INVOICE
          && start.identityType() != IdentityDocumentType.RUC) {
        return result(context, "Una factura requiere receptor con RUC.", "START_REJECTED");
      }
      if ((start.identityType() == IdentityDocumentType.DNI && start.documentNumber().length() != 8)
          || (start.identityType() == IdentityDocumentType.RUC
              && start.documentNumber().length() != 11)) {
        return result(context, "El número no coincide con el tipo de documento.", "START_REJECTED");
      }
      var updated = context.start(start.documentType(), start.identityType(),
          start.documentNumber(), start.currency(), now);
      return result(updated, "Comprobante iniciado. Agrega productos con: "
          + "AGREGAR cantidad | descripción | precio", "START");
    }
    if (command instanceof ConversationCommandParser.AddItemCommand item) {
      try {
        var updated = context.addItem(ConversationDraftItem.create(
            item.description(), item.quantity(), item.unitPrice()), now);
        return result(updated, "Ítem agregado. Tienes " + updated.items().size()
            + " ítem(s). Usa RESUMEN o GENERAR.", "ADD_ITEM");
      } catch (IllegalArgumentException | IllegalStateException exception) {
        return result(context, exception.getMessage(), "ADD_ITEM_REJECTED");
      }
    }
    if (command instanceof ConversationCommandParser.SummaryCommand) {
      if (context.state() == com.invoiceautomationservice.domain.model.ConversationFlowState.EMPTY) {
        return result(context, "No hay un comprobante en preparación. " + HELP, "SUMMARY_EMPTY");
      }
      BigDecimal total = context.items().stream()
          .map(item -> item.quantity().multiply(item.unitPrice()))
          .reduce(BigDecimal.ZERO, BigDecimal::add);
      return result(context, context.documentType() + " para " + context.recipientDocumentType()
          + " " + context.recipientDocumentNumber() + ", " + context.items().size()
          + " ítem(s), importe referencial " + context.currency() + " " + total.toPlainString()
          + (context.invoiceDraftId() == null ? "." : ", borrador " + context.invoiceDraftId()),
          "SUMMARY");
    }
    if (command instanceof ConversationCommandParser.GenerateCommand) {
      try {
        context.ensureCollecting();
        if (context.items().isEmpty()) {
          return result(context, "Agrega al menos un ítem antes de generar.", "GENERATE_REJECTED");
        }
        var request = new CreateInvoiceDraftRequest(companyId, context.documentType(),
            context.recipientDocumentType(), context.recipientDocumentNumber(), context.currency(),
            context.items().stream().map(this::toRequest).toList());
        var draft = invoiceDraftUseCase.create(request);
        var updated = context.markDraftCreated(draft.id(), now);
        return result(updated, "Borrador creado: " + draft.id()
            + ". Revísalo y apruébalo antes de emitir.", "GENERATE");
      } catch (IllegalStateException exception) {
        return result(context, exception.getMessage(), "GENERATE_REJECTED");
      }
    }
    if (command instanceof ConversationCommandParser.ResetCommand) {
      return result(context.reset(now), "Flujo conversacional reiniciado.", "RESET");
    }
    return result(context, "No entendí el comando. " + HELP, "UNKNOWN");
  }

  private CreateInvoiceItemRequest toRequest(ConversationDraftItem item) {
    return new CreateInvoiceItemRequest(item.description(), item.unitCode(), item.quantity(),
        item.unitPrice(), item.discount(), item.taxAffectation());
  }

  private ProcessingResult result(ConversationContext context, String reply, String commandName) {
    return new ProcessingResult(context, reply, commandName);
  }

  private ConversationContextResponse toResponse(ConversationContext context) {
    return new ConversationContextResponse(context.state(), context.documentType(),
        context.recipientDocumentType(), context.recipientDocumentNumber(), context.currency(),
        context.items().size(), context.invoiceDraftId());
  }

  private record ProcessingResult(
      ConversationContext context, String reply, String commandName) {}
}
