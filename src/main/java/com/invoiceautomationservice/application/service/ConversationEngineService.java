package com.invoiceautomationservice.application.service;

import com.invoiceautomationservice.application.dto.request.CreateInvoiceDraftRequest;
import com.invoiceautomationservice.application.dto.request.CreateInvoiceItemRequest;
import com.invoiceautomationservice.application.dto.response.ConversationContextResponse;
import com.invoiceautomationservice.application.dto.response.ConversationEngineResponse;
import com.invoiceautomationservice.application.dto.response.ConversationReviewResponse;
import com.invoiceautomationservice.application.port.in.ConversationEngineUseCase;
import com.invoiceautomationservice.application.port.in.InvoiceDraftUseCase;
import com.invoiceautomationservice.application.port.out.ConversationContextRepository;
import com.invoiceautomationservice.application.port.out.ConversationRepository;
import com.invoiceautomationservice.application.port.out.DocumentUnderstandingProvider;
import com.invoiceautomationservice.application.port.out.MessageRepository;
import com.invoiceautomationservice.domain.model.AuditAction;
import com.invoiceautomationservice.domain.model.CompanyPermission;
import com.invoiceautomationservice.domain.model.ConversationContext;
import com.invoiceautomationservice.domain.model.ConversationDraftItem;
import com.invoiceautomationservice.domain.model.ConversationFlowState;
import com.invoiceautomationservice.domain.model.DocumentInterpretation;
import com.invoiceautomationservice.domain.model.IdentityDocumentType;
import com.invoiceautomationservice.domain.model.InterpretationContextSnapshot;
import com.invoiceautomationservice.domain.model.InterpretationIntent;
import com.invoiceautomationservice.domain.model.InterpretedInvoiceItem;
import com.invoiceautomationservice.domain.model.InvoiceDocumentType;
import com.invoiceautomationservice.domain.model.Message;
import com.invoiceautomationservice.domain.model.MessageDirection;
import com.invoiceautomationservice.domain.model.MessageStatus;
import com.invoiceautomationservice.domain.model.MessageType;
import com.invoiceautomationservice.domain.model.TextInterpretationInput;
import com.invoiceautomationservice.infrastructure.config.AiProperties;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ConversationEngineService implements ConversationEngineUseCase {
  private static final String HELP = "Comandos: NUEVA BOLETA DNI 12345678 PEN; "
      + "NUEVA FACTURA RUC 20123456789 PEN; AGREGAR cantidad | descripción | precio; "
      + "RESUMEN; CONFIRMAR; GENERAR; CANCELAR.";

  private final ConversationRepository conversationRepository;
  private final ConversationContextRepository contextRepository;
  private final MessageRepository messageRepository;
  private final InvoiceDraftUseCase invoiceDraftUseCase;
  private final CompanyAccessService accessService;
  private final ConversationCommandParser parser;
  private final DocumentUnderstandingProvider understandingProvider;
  private final AiProperties aiProperties;
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
    ConversationCommandParser.Command command = parser.parse(text);
    ProcessingResult result = command instanceof ConversationCommandParser.UnknownCommand
        ? executeNatural(conversation.companyId(), context, text, now)
        : execute(conversation.companyId(), context, command, now);
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
        return result(context, "El número no coincide con el tipo de documento.",
            "START_REJECTED");
      }
      ConversationContext updated = context.start(start.documentType(), start.identityType(),
          start.documentNumber(), start.currency(), now);
      return result(updated, "Datos del comprobante confirmados. Agrega productos con: "
          + "AGREGAR cantidad | descripción | precio", "START");
    }
    if (command instanceof ConversationCommandParser.AddItemCommand item) {
      try {
        ConversationContext updated = context.addItem(ConversationDraftItem.create(
            item.description(), item.quantity(), item.unitPrice()), now);
        return result(updated, "Ítem confirmado. Tienes " + updated.items().size()
            + " ítem(s). " + readyInstruction(updated), "ADD_ITEM");
      } catch (IllegalArgumentException | IllegalStateException exception) {
        return result(context, exception.getMessage(), "ADD_ITEM_REJECTED");
      }
    }
    if (command instanceof ConversationCommandParser.SummaryCommand) {
      if (context.state() == ConversationFlowState.EMPTY) {
        return result(context, "No hay un comprobante en preparación. " + HELP,
            "SUMMARY_EMPTY");
      }
      BigDecimal total = calculateTotal(context);
      String review = context.reviewRequired()
          ? " Requiere confirmación o corrección antes de generar." : "";
      return result(context, "Estado " + context.state() + ": " + context.documentType()
          + " para " + context.recipientDocumentType() + " "
          + context.recipientDocumentNumber() + ", " + context.items().size()
          + " ítem(s), importe referencial " + context.currency() + " " + total.toPlainString()
          + (context.invoiceDraftId() == null ? "." : ", borrador " + context.invoiceDraftId())
          + review, "SUMMARY");
    }
    if (command instanceof ConversationCommandParser.ConfirmCommand) {
      try {
        ConversationContext updated = context.confirmInterpretation(now);
        return result(updated, updated.state() == ConversationFlowState.READY_TO_CREATE
            ? "Datos confirmados. Usa GENERAR para crear el borrador."
            : "Datos confirmados. Continúa proporcionando los datos pendientes.",
            "CONFIRM_INTERPRETATION");
      } catch (IllegalStateException exception) {
        return result(context, exception.getMessage(), "CONFIRM_REJECTED");
      }
    }
    if (command instanceof ConversationCommandParser.GenerateCommand) {
      try {
        context.ensureReadyToCreate();
        var request = new CreateInvoiceDraftRequest(companyId, context.documentType(),
            context.recipientDocumentType(), context.recipientDocumentNumber(), context.currency(),
            context.items().stream().map(this::toRequest).toList());
        var draft = invoiceDraftUseCase.create(request);
        ConversationContext updated = context.markDraftCreated(draft.id(), now);
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

  private ProcessingResult executeNatural(
      String companyId, ConversationContext context, String text, Instant now) {
    if (context.state() == ConversationFlowState.PROCESSING_MEDIA) {
      return result(context,
          "La imagen todavía se está procesando. Consulta nuevamente en unos segundos.",
          "MEDIA_PROCESSING");
    }
    if (context.state() == ConversationFlowState.DRAFT_CREATED) {
      return result(context,
          "El borrador ya fue creado. Usa NUEVA BOLETA, NUEVA FACTURA o CANCELAR para otro flujo.",
          "DRAFT_ALREADY_CREATED");
    }
    DocumentInterpretation interpretation = understandingProvider.interpretText(
        new TextInterpretationInput(companyId, context.conversationId(), text,
            toInterpretationContext(context)));
    if (interpretation.intent() == InterpretationIntent.UNKNOWN) {
      return result(context, "No entendí el mensaje. Puedes escribir naturalmente o usar: "
          + HELP, "NATURAL_UNKNOWN");
    }
    if (interpretation.hasMissingFields()) {
      ConversationContext updated = context.stageInterpretation(
          interpretation, ConversationFlowState.COLLECTING_DATA, now);
      return result(updated, "Entendí parcialmente la solicitud. Falta indicar: "
          + describeFields(interpretation.missingFields()) + ".", "NATURAL_INCOMPLETE");
    }
    if (!interpretation.ambiguousFields().isEmpty()) {
      ConversationContext updated = context.stageInterpretation(
          interpretation, ConversationFlowState.NEEDS_REVIEW, now);
      return result(updated, "Encontré valores ambiguos en: "
          + describeFields(interpretation.ambiguousFields())
          + ". Indica el valor correcto antes de continuar.", "NATURAL_AMBIGUOUS");
    }
    if (interpretation.documentType() == InvoiceDocumentType.INVOICE
        && interpretation.recipientDocumentType() == IdentityDocumentType.DNI) {
      ConversationContext updated = context.stageInterpretation(
          interpretation, ConversationFlowState.NEEDS_REVIEW, now);
      return result(updated,
          "Detecté una factura con DNI, pero una factura requiere receptor con RUC. Corrige el dato.",
          "NATURAL_INVALID_RECIPIENT");
    }
    if (interpretation.requiresReview(aiProperties.getReviewThreshold())) {
      ConversationContext updated = context.stageInterpretation(
          interpretation, ConversationFlowState.NEEDS_REVIEW, now);
      String reason = !interpretation.calculationErrors().isEmpty()
          ? "Detecté una inconsistencia de cálculo: "
              + String.join("; ", interpretation.calculationErrors())
          : "La confianza de la interpretación es " + interpretation.confidence();
      return result(updated, reason
          + ". Responde CONFIRMAR para aceptar los valores detectados o envía la corrección.",
          "NATURAL_NEEDS_REVIEW");
    }
    return applyConfirmedInterpretation(context, interpretation, now);
  }

  private ProcessingResult applyConfirmedInterpretation(
      ConversationContext context, DocumentInterpretation interpretation, Instant now) {
    try {
      if (interpretation.intent() == InterpretationIntent.START_DOCUMENT) {
        ConversationContext updated = context.applyHeader(interpretation, now);
        return result(updated, "Datos del comprobante detectados y confirmados. "
            + "Puedes describir los productos, cantidades y precios.", "NATURAL_START");
      }
      if (interpretation.intent() == InterpretationIntent.ADD_ITEM) {
        ConversationContext updated = context.applyItems(interpretation, now);
        BigDecimal accumulatedTotal = calculateTotal(updated);
        return result(updated, "Ítem detectado y confirmado. Total acumulado "
            + updated.currency() + " "
            + accumulatedTotal.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString()
            + ". " + readyInstruction(updated), "NATURAL_ADD_ITEM");
      }
      return result(context, "La interpretación no puede aplicarse automáticamente.",
          "NATURAL_UNSUPPORTED");
    } catch (IllegalArgumentException | IllegalStateException exception) {
      return result(context, exception.getMessage(), "NATURAL_REJECTED");
    }
  }

  private InterpretationContextSnapshot toInterpretationContext(ConversationContext context) {
    DocumentInterpretation detected = context.lastInterpretation();
    InvoiceDocumentType type = context.documentType() != null ? context.documentType()
        : detected == null ? null : detected.documentType();
    IdentityDocumentType identityType = context.recipientDocumentType() != null
        ? context.recipientDocumentType()
        : detected == null ? null : detected.recipientDocumentType();
    String number = context.recipientDocumentNumber() != null ? context.recipientDocumentNumber()
        : detected == null ? null : detected.recipientDocumentNumber();
    return new InterpretationContextSnapshot(type, identityType, number, context.currency(),
        context.items().stream().map(item -> new InterpretedInvoiceItem(
            item.description(), item.unitCode(), item.quantity(), item.unitPrice(),
            item.discount(), item.taxAffectation(),
            item.quantity().multiply(item.unitPrice()), BigDecimal.ONE, List.of())).toList());
  }

  private String describeFields(List<String> fields) {
    return fields.stream().map(field -> switch (field) {
      case "documentType" -> "si es boleta o factura";
      case "recipientDocumentType" -> "si el receptor usa DNI o RUC";
      case "recipientDocumentNumber" -> "el número de DNI o RUC";
      case "recipientDocument" -> "el tipo y número de documento del receptor";
      case "items[0].description" -> "la descripción";
      case "items[0].quantity" -> "la cantidad";
      case "items[0].unitPrice" -> "el precio unitario";
      default -> field;
    }).distinct().collect(java.util.stream.Collectors.joining(", "));
  }

  private BigDecimal calculateTotal(ConversationContext context) {
    return context.items().stream()
        .map(item -> item.quantity().multiply(item.unitPrice()))
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  private String readyInstruction(ConversationContext context) {
    return context.state() == ConversationFlowState.READY_TO_CREATE
        ? "Usa RESUMEN o GENERAR." : "Continúa proporcionando los datos faltantes.";
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
        context.items().size(), context.invoiceDraftId(), toReviewResponse(context));
  }

  private ConversationReviewResponse toReviewResponse(ConversationContext context) {
    DocumentInterpretation interpretation = context.lastInterpretation();
    return new ConversationReviewResponse(
        interpretation == null ? Map.of() : detectedValues(interpretation),
        confirmedValues(context),
        interpretation == null ? List.of() : interpretation.missingFields(),
        interpretation == null ? List.of() : interpretation.ambiguousFields(),
        interpretation == null ? BigDecimal.ONE : interpretation.confidence(),
        interpretation == null ? List.of() : interpretation.calculationErrors(),
        context.reviewRequired());
  }

  private Map<String, String> detectedValues(DocumentInterpretation value) {
    Map<String, String> result = new LinkedHashMap<>();
    put(result, "documentType", value.documentType());
    put(result, "recipientDocumentType", value.recipientDocumentType());
    put(result, "recipientDocumentNumber", value.recipientDocumentNumber());
    put(result, "currency", value.currency());
    put(result, "reportedTotal", value.reportedTotal());
    for (int index = 0; index < value.items().size(); index++) {
      InterpretedInvoiceItem item = value.items().get(index);
      String prefix = "items[" + index + "].";
      put(result, prefix + "description", item.description());
      put(result, prefix + "quantity", item.quantity());
      put(result, prefix + "unitPrice", item.unitPrice());
    }
    return result;
  }

  private Map<String, String> confirmedValues(ConversationContext context) {
    Map<String, String> result = new LinkedHashMap<>();
    put(result, "documentType", context.documentType());
    put(result, "recipientDocumentType", context.recipientDocumentType());
    put(result, "recipientDocumentNumber", context.recipientDocumentNumber());
    put(result, "currency", context.currency());
    for (int index = 0; index < context.items().size(); index++) {
      ConversationDraftItem item = context.items().get(index);
      String prefix = "items[" + index + "].";
      put(result, prefix + "description", item.description());
      put(result, prefix + "quantity", item.quantity());
      put(result, prefix + "unitPrice", item.unitPrice());
    }
    return result;
  }

  private void put(Map<String, String> values, String field, Object value) {
    if (value != null) values.put(field, value.toString());
  }

  private record ProcessingResult(
      ConversationContext context, String reply, String commandName) {}
}
