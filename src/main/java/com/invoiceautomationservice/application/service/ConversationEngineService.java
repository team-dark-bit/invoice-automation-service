package com.invoiceautomationservice.application.service;

import com.invoiceautomationservice.application.dto.request.CreateInvoiceDraftRequest;
import com.invoiceautomationservice.application.dto.request.CreateInvoiceItemRequest;
import com.invoiceautomationservice.application.dto.request.UpdateInvoiceDraftRequest;
import com.invoiceautomationservice.application.dto.response.ConversationContextResponse;
import com.invoiceautomationservice.application.dto.response.ConversationEngineResponse;
import com.invoiceautomationservice.application.dto.response.ConversationReviewResponse;
import com.invoiceautomationservice.application.dto.response.InvoiceDraftResponse;
import com.invoiceautomationservice.application.port.in.ConversationEngineUseCase;
import com.invoiceautomationservice.application.port.in.InvoiceDraftUseCase;
import com.invoiceautomationservice.application.port.out.ConversationContextRepository;
import com.invoiceautomationservice.application.port.out.ConversationRepository;
import com.invoiceautomationservice.application.port.out.DocumentUnderstandingProvider;
import com.invoiceautomationservice.application.port.out.MessageRepository;
import com.invoiceautomationservice.domain.exception.InvalidInvoiceDraftStateException;
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
import com.invoiceautomationservice.domain.model.InvoiceDraftStatus;
import com.invoiceautomationservice.domain.model.Message;
import com.invoiceautomationservice.domain.model.MessageDirection;
import com.invoiceautomationservice.domain.model.MessageStatus;
import com.invoiceautomationservice.domain.model.MessageType;
import com.invoiceautomationservice.domain.model.TextInterpretationInput;
import com.invoiceautomationservice.infrastructure.config.AiProperties;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.text.Normalizer;
import java.util.ArrayList;
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
      + "RESUMEN; GENERAR para revisar; CONFIRMAR para crear el borrador; CANCELAR. "
      + "Correcciones: CAMBIA LA CANTIDAD DE "
      + "producto A valor; CAMBIA EL PRECIO DE producto A valor; ELIMINA producto; "
      + "EL DNI/RUC CORRECTO ES número; ES FACTURA/BOLETA.";

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
    if (isCorrection(command)) {
      return applyCorrection(context, command, now);
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
      if (context.state() == ConversationFlowState.AWAITING_DRAFT_CONFIRMATION) {
        return createConfirmedDraft(companyId, context, now);
      }
      try {
        ConversationContext updated = context.confirmInterpretation(now);
        return result(updated, updated.state() == ConversationFlowState.READY_TO_CREATE
            ? "Datos confirmados. Usa GENERAR para revisar el resumen antes de crear el borrador."
            : "Datos confirmados. Continúa proporcionando los datos pendientes.",
            "CONFIRM_INTERPRETATION");
      } catch (IllegalStateException exception) {
        return result(context, exception.getMessage(), "CONFIRM_REJECTED");
      }
    }
    if (command instanceof ConversationCommandParser.GenerateCommand) {
      try {
        ConversationContext awaiting = context.state()
            == ConversationFlowState.AWAITING_DRAFT_CONFIRMATION
            ? context : context.requestDraftConfirmation(now);
        return result(awaiting, draftConfirmationSummary(awaiting),
            "REQUEST_DRAFT_CONFIRMATION");
      } catch (IllegalStateException exception) {
        return result(context, exception.getMessage(), "GENERATE_REJECTED");
      }
    }
    if (command instanceof ConversationCommandParser.ResetCommand) {
      return result(context.reset(now), "Flujo conversacional reiniciado.", "RESET");
    }
    return result(context, "No entendí el comando. " + HELP, "UNKNOWN");
  }

  private ProcessingResult createConfirmedDraft(
      String companyId, ConversationContext context, Instant now) {
    try {
      context.ensureAwaitingDraftConfirmation();
      var request = new CreateInvoiceDraftRequest(companyId, context.documentType(),
          context.recipientDocumentType(), context.recipientDocumentNumber(), context.currency(),
          context.items().stream().map(this::toRequest).toList());
      var draft = invoiceDraftUseCase.create(request);
      ConversationContext updated = context.markDraftCreated(draft.id(), now);
      return result(updated, "Borrador creado: " + draft.id()
          + ". Revísalo y apruébalo antes de emitir.", "CONFIRM_DRAFT_CREATION");
    } catch (IllegalStateException exception) {
      return result(context, exception.getMessage(), "CONFIRM_DRAFT_REJECTED");
    }
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

  private boolean isCorrection(ConversationCommandParser.Command command) {
    return command instanceof ConversationCommandParser.ChangeItemQuantityCommand
        || command instanceof ConversationCommandParser.ChangeItemPriceCommand
        || command instanceof ConversationCommandParser.RemoveItemCommand
        || command instanceof ConversationCommandParser.CorrectRecipientCommand
        || command instanceof ConversationCommandParser.CorrectDocumentTypeCommand;
  }

  private ProcessingResult applyCorrection(
      ConversationContext context, ConversationCommandParser.Command command, Instant now) {
    if (context.state() == ConversationFlowState.PROCESSING_MEDIA) {
      return result(context, "Espera a que termine el procesamiento de la imagen.",
          "CORRECTION_MEDIA_PROCESSING");
    }
    if (context.state() == ConversationFlowState.DRAFT_CREATED) {
      return applyDraftCorrection(context, command, now);
    }
    try {
      if (command instanceof ConversationCommandParser.ChangeItemQuantityCommand correction) {
        if (correction.quantity().signum() <= 0) {
          throw new CorrectionException("La cantidad debe ser mayor que cero.");
        }
        return correctContextItem(context, correction.itemReference(), correction.quantity(),
            null, false, now);
      }
      if (command instanceof ConversationCommandParser.ChangeItemPriceCommand correction) {
        if (correction.unitPrice().signum() < 0) {
          throw new CorrectionException("El precio no puede ser negativo.");
        }
        return correctContextItem(context, correction.itemReference(), null,
            correction.unitPrice(), false, now);
      }
      if (command instanceof ConversationCommandParser.RemoveItemCommand correction) {
        return correctContextItem(context, correction.itemReference(), null, null, true, now);
      }
      if (command instanceof ConversationCommandParser.CorrectRecipientCommand correction) {
        return correctContextRecipient(context, correction, now);
      }
      if (command instanceof ConversationCommandParser.CorrectDocumentTypeCommand correction) {
        return correctContextDocumentType(context, correction.documentType(), now);
      }
      throw new CorrectionException("No se reconoció la corrección.");
    } catch (CorrectionException | IllegalArgumentException | IllegalStateException exception) {
      return result(context, exception.getMessage(), "CORRECTION_REJECTED");
    }
  }

  private ProcessingResult correctContextItem(
      ConversationContext context, String reference, BigDecimal quantity, BigDecimal unitPrice,
      boolean remove, Instant now) {
    ItemTarget target = resolveContextItem(context, reference);
    if (target.pending()) {
      DocumentInterpretation interpretation = context.lastInterpretation();
      if (remove) {
        if (interpretation.items().size() == 1) {
          ConversationContext updated = context.discardPendingInterpretation(now);
          return result(updated, "Producto detectado eliminado. " + readyInstruction(updated),
              "CORRECT_PENDING_ITEM_REMOVED");
        }
        return finalizeCorrectedInterpretation(context,
            interpretation.removeItem(target.index()), now, "Producto detectado eliminado.");
      }
      DocumentInterpretation corrected = quantity != null
          ? interpretation.correctItemQuantity(target.index(), quantity)
          : interpretation.correctItemUnitPrice(target.index(), unitPrice);
      return finalizeCorrectedInterpretation(context, corrected, now,
          quantity != null ? "Cantidad corregida." : "Precio corregido.");
    }

    var correctedItems = new ArrayList<>(context.items());
    if (remove) {
      correctedItems.remove(target.index());
    } else {
      ConversationDraftItem current = correctedItems.get(target.index());
      correctedItems.set(target.index(), quantity != null
          ? current.changeQuantity(quantity) : current.changeUnitPrice(unitPrice));
    }
    ConversationContext updated = context.replaceItems(correctedItems, now);
    return result(updated, remove
        ? "Producto eliminado. " + readyInstruction(updated)
        : (quantity != null ? "Cantidad corregida. " : "Precio corregido. ")
            + totalReply(updated), "CORRECT_CONTEXT_ITEM");
  }

  private ProcessingResult correctContextRecipient(ConversationContext context,
      ConversationCommandParser.CorrectRecipientCommand correction, Instant now) {
    validateDocumentNumber(correction.identityType(), correction.documentNumber());
    if (context.documentType() == InvoiceDocumentType.INVOICE
        && correction.identityType() != IdentityDocumentType.RUC) {
      throw new CorrectionException("Una factura requiere receptor con RUC.");
    }
    DocumentInterpretation pending = pendingInterpretation(context);
    if (pending != null && pending.intent() == InterpretationIntent.START_DOCUMENT) {
      return finalizeCorrectedInterpretation(context,
          pending.correctRecipient(correction.identityType(), correction.documentNumber()), now,
          "Documento del receptor corregido.");
    }
    ConversationContext updated = context.correctRecipient(
        correction.identityType(), correction.documentNumber(), now);
    return result(updated, "Documento del receptor corregido. " + readyInstruction(updated),
        "CORRECT_RECIPIENT");
  }

  private ProcessingResult correctContextDocumentType(
      ConversationContext context, InvoiceDocumentType type, Instant now) {
    DocumentInterpretation pending = pendingInterpretation(context);
    if (pending != null && pending.intent() == InterpretationIntent.START_DOCUMENT) {
      return finalizeCorrectedInterpretation(context, pending.correctDocumentType(type), now,
          "Tipo de comprobante corregido.");
    }
    ConversationContext updated = context.correctDocumentType(type, now);
    String reply = type == InvoiceDocumentType.INVOICE
        && updated.recipientDocumentType() == null
        ? "Tipo corregido a factura. Indica el RUC correcto del receptor."
        : "Tipo de comprobante corregido. " + readyInstruction(updated);
    return result(updated, reply, "CORRECT_DOCUMENT_TYPE");
  }

  private ProcessingResult finalizeCorrectedInterpretation(
      ConversationContext context, DocumentInterpretation corrected, Instant now, String prefix) {
    if (corrected.hasMissingFields()) {
      ConversationContext updated = context.stageInterpretation(
          corrected, ConversationFlowState.COLLECTING_DATA, now);
      return result(updated, prefix + " Falta indicar: "
          + describeFields(corrected.missingFields()) + ".", "CORRECTION_INCOMPLETE");
    }
    if (!corrected.ambiguousFields().isEmpty()) {
      ConversationContext updated = context.stageInterpretation(
          corrected, ConversationFlowState.NEEDS_REVIEW, now);
      return result(updated, prefix + " Aún existen valores ambiguos en: "
          + describeFields(corrected.ambiguousFields()) + ".", "CORRECTION_AMBIGUOUS");
    }
    if (corrected.documentType() == InvoiceDocumentType.INVOICE
        && corrected.recipientDocumentType() != IdentityDocumentType.RUC) {
      DocumentInterpretation awaitingRuc = corrected.requireRucRecipient();
      ConversationContext updated = context.stageInterpretation(
          awaitingRuc, ConversationFlowState.COLLECTING_DATA, now);
      return result(updated, prefix + " Una factura requiere indicar un RUC.",
          "CORRECTION_REQUIRES_RUC");
    }
    if (!corrected.calculationErrors().isEmpty()) {
      ConversationContext updated = context.stageInterpretation(
          corrected, ConversationFlowState.NEEDS_REVIEW, now);
      return result(updated, prefix + " Persiste una inconsistencia: "
          + String.join("; ", corrected.calculationErrors())
          + ". Corrige nuevamente o responde CONFIRMAR.", "CORRECTION_CALCULATION_REVIEW");
    }
    ProcessingResult applied = applyConfirmedInterpretation(context, corrected, now);
    return result(applied.context(), prefix + " " + applied.reply(), "CORRECTION_APPLIED");
  }

  private ItemTarget resolveContextItem(ConversationContext context, String reference) {
    DocumentInterpretation pending = pendingInterpretation(context);
    List<ItemTarget> targets = new ArrayList<>();
    if (pending != null && pending.intent() == InterpretationIntent.ADD_ITEM) {
      for (int index = 0; index < pending.items().size(); index++) {
        targets.add(new ItemTarget(true, index, pending.items().get(index).description()));
      }
    }
    for (int index = 0; index < context.items().size(); index++) {
      targets.add(new ItemTarget(false, index, context.items().get(index).description()));
    }
    if (reference == null || reference.isBlank()) {
      List<ItemTarget> pendingTargets = targets.stream().filter(ItemTarget::pending).toList();
      if (pendingTargets.size() == 1) return pendingTargets.getFirst();
      if (targets.size() == 1) return targets.getFirst();
      throw new CorrectionException(
          "Indica el producto cuyo precio o cantidad deseas cambiar.");
    }
    String normalized = normalizeForMatch(reference);
    List<ItemTarget> exact = targets.stream()
        .filter(target -> normalizeForMatch(target.description()).equals(normalized)).toList();
    if (exact.size() == 1) return exact.getFirst();
    List<ItemTarget> matches = targets.stream()
        .filter(target -> normalizeForMatch(target.description()).contains(normalized)
            || normalized.contains(normalizeForMatch(target.description())))
        .toList();
    if (matches.isEmpty()) {
      throw new CorrectionException("No encontré un producto que coincida con " + reference + ".");
    }
    if (matches.size() > 1) {
      throw new CorrectionException("La referencia coincide con varios productos: "
          + matches.stream().map(ItemTarget::description).distinct()
              .collect(java.util.stream.Collectors.joining(", ")) + ".");
    }
    return matches.getFirst();
  }

  private DocumentInterpretation pendingInterpretation(ConversationContext context) {
    DocumentInterpretation value = context.lastInterpretation();
    if (value == null) return null;
    return context.reviewRequired() || value.hasMissingFields()
        || !value.ambiguousFields().isEmpty() ? value : null;
  }

  private ProcessingResult applyDraftCorrection(
      ConversationContext context, ConversationCommandParser.Command command, Instant now) {
    InvoiceDraftResponse draft = invoiceDraftUseCase.findById(context.invoiceDraftId());
    if (draft.status() != InvoiceDraftStatus.DRAFT) {
      return result(context, "El borrador ya no está en estado DRAFT y no puede corregirse.",
          "DRAFT_CORRECTION_REJECTED");
    }
    try {
      InvoiceDocumentType type = draft.documentType();
      IdentityDocumentType identityType = draft.recipientDocumentType();
      String documentNumber = draft.recipientDocumentNumber();
      List<CreateInvoiceItemRequest> items = draft.items().stream().map(item ->
          new CreateInvoiceItemRequest(item.description(), item.unitCode(), item.quantity(),
              item.unitPrice(), item.discount(), item.taxAffectation())).toList();
      var correctedItems = new ArrayList<>(items);

      if (command instanceof ConversationCommandParser.ChangeItemQuantityCommand correction) {
        if (correction.quantity().signum() <= 0) {
          throw new CorrectionException("La cantidad debe ser mayor que cero.");
        }
        int index = resolveItemIndex(correctedItems, correction.itemReference(), false);
        CreateInvoiceItemRequest current = correctedItems.get(index);
        correctedItems.set(index, copyItem(current, correction.quantity(), current.unitPrice()));
      } else if (command instanceof ConversationCommandParser.ChangeItemPriceCommand correction) {
        if (correction.unitPrice().signum() < 0) {
          throw new CorrectionException("El precio no puede ser negativo.");
        }
        int index = resolveItemIndex(correctedItems, correction.itemReference(), true);
        CreateInvoiceItemRequest current = correctedItems.get(index);
        correctedItems.set(index, copyItem(current, current.quantity(), correction.unitPrice()));
      } else if (command instanceof ConversationCommandParser.RemoveItemCommand correction) {
        int index = resolveItemIndex(correctedItems, correction.itemReference(), false);
        if (correctedItems.size() == 1) {
          throw new CorrectionException("El borrador debe conservar al menos un producto.");
        }
        correctedItems.remove(index);
      } else if (command instanceof ConversationCommandParser.CorrectRecipientCommand correction) {
        validateDocumentNumber(correction.identityType(), correction.documentNumber());
        if (type == InvoiceDocumentType.INVOICE
            && correction.identityType() != IdentityDocumentType.RUC) {
          throw new CorrectionException("Una factura requiere receptor con RUC.");
        }
        identityType = correction.identityType();
        documentNumber = correction.documentNumber();
      } else if (command instanceof ConversationCommandParser.CorrectDocumentTypeCommand correction) {
        if (correction.documentType() == InvoiceDocumentType.INVOICE
            && identityType != IdentityDocumentType.RUC) {
          throw new CorrectionException(
              "Para cambiar el borrador a factura, primero indica el RUC correcto.");
        }
        type = correction.documentType();
      }

      InvoiceDraftResponse updated = invoiceDraftUseCase.update(draft.id(),
          new UpdateInvoiceDraftRequest(type, identityType, documentNumber, draft.currency(),
              correctedItems));
      ConversationContext synchronizedContext = context.synchronizeDraft(updated.documentType(),
          updated.recipientDocumentType(), updated.recipientDocumentNumber(), updated.currency(),
          updated.items().stream().map(item -> new ConversationDraftItem(item.id(),
              item.description(), item.unitCode(), item.quantity(), item.unitPrice(),
              item.discount(), item.taxAffectation())).toList(), now);
      return result(synchronizedContext, "Borrador corregido. Nuevo total " + updated.currency() + " "
          + updated.total().setScale(2, java.math.RoundingMode.HALF_UP).toPlainString() + ".",
          "DRAFT_CORRECTED");
    } catch (CorrectionException | InvalidInvoiceDraftStateException | IllegalArgumentException exception) {
      return result(context, exception.getMessage(), "DRAFT_CORRECTION_REJECTED");
    }
  }

  private int resolveItemIndex(
      List<CreateInvoiceItemRequest> items, String reference, boolean allowImplicit) {
    if (reference == null || reference.isBlank()) {
      if (allowImplicit && items.size() == 1) return 0;
      throw new CorrectionException("Indica el producto que deseas corregir.");
    }
    String normalized = normalizeForMatch(reference);
    List<Integer> exact = java.util.stream.IntStream.range(0, items.size()).boxed()
        .filter(index -> normalizeForMatch(items.get(index).description()).equals(normalized))
        .toList();
    if (exact.size() == 1) return exact.getFirst();
    List<Integer> matches = java.util.stream.IntStream.range(0, items.size()).boxed()
        .filter(index -> normalizeForMatch(items.get(index).description()).contains(normalized)
            || normalized.contains(normalizeForMatch(items.get(index).description())))
        .toList();
    if (matches.isEmpty()) throw new CorrectionException(
        "No encontré un producto que coincida con " + reference + ".");
    if (matches.size() > 1) throw new CorrectionException(
        "La referencia coincide con varios productos. Especifica el nombre completo.");
    return matches.getFirst();
  }

  private CreateInvoiceItemRequest copyItem(
      CreateInvoiceItemRequest item, BigDecimal quantity, BigDecimal unitPrice) {
    return new CreateInvoiceItemRequest(item.description(), item.unitCode(), quantity, unitPrice,
        item.discount(), item.taxAffectation());
  }

  private void validateDocumentNumber(IdentityDocumentType type, String number) {
    if (number == null || !number.matches("\\d{" + type.length() + "}")) {
      throw new CorrectionException("El número no coincide con el tipo de documento.");
    }
  }

  private String normalizeForMatch(String value) {
    return Normalizer.normalize(value.strip().toLowerCase(java.util.Locale.ROOT),
        Normalizer.Form.NFD).replaceAll("\\p{M}", "").replaceAll("\\s+", " ");
  }

  private String totalReply(ConversationContext context) {
    return "Total acumulado " + context.currency() + " "
        + calculateTotal(context).setScale(2, java.math.RoundingMode.HALF_UP).toPlainString()
        + ". " + readyInstruction(context);
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

  private String draftConfirmationSummary(ConversationContext context) {
    String currency = currencySymbol(context.currency());
    StringBuilder summary = new StringBuilder("Detecté:\n- ")
        .append(context.documentType() == InvoiceDocumentType.SALES_RECEIPT
            ? "Boleta" : "Factura")
        .append(" para ").append(context.recipientDocumentType()).append(" ")
        .append(context.recipientDocumentNumber());
    context.items().forEach(item -> summary.append("\n- ")
        .append(item.quantity().stripTrailingZeros().toPlainString()).append(" ")
        .append(item.description()).append(" × ").append(currency).append(" ")
        .append(item.unitPrice().setScale(2, java.math.RoundingMode.HALF_UP).toPlainString()));
    return summary.append("\n- Total calculado: ").append(currency).append(" ")
        .append(calculateTotal(context).setScale(2, java.math.RoundingMode.HALF_UP).toPlainString())
        .append("\n\n¿Confirmas la creación del borrador? Responde CONFIRMAR o envía una corrección.")
        .toString();
  }

  private String currencySymbol(String currency) {
    return switch (currency) {
      case "PEN" -> "S/";
      case "USD" -> "$";
      default -> currency;
    };
  }

  private String readyInstruction(ConversationContext context) {
    return context.state() == ConversationFlowState.READY_TO_CREATE
        ? "Usa GENERAR para revisar el resumen antes de confirmar."
        : "Continúa proporcionando los datos faltantes.";
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

  private record ItemTarget(boolean pending, int index, String description) {}

  private static final class CorrectionException extends RuntimeException {
    private CorrectionException(String message) {
      super(message);
    }
  }
}
