package com.invoiceautomationservice.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

import com.invoiceautomationservice.application.dto.response.InvoiceDraftResponse;
import com.invoiceautomationservice.application.dto.response.InvoiceItemResponse;
import com.invoiceautomationservice.application.dto.request.UpdateInvoiceDraftRequest;
import com.invoiceautomationservice.application.port.in.InvoiceDraftUseCase;
import com.invoiceautomationservice.application.port.out.ConversationContextRepository;
import com.invoiceautomationservice.application.port.out.ConversationRepository;
import com.invoiceautomationservice.application.port.out.MessageRepository;
import com.invoiceautomationservice.domain.model.CompanyPermission;
import com.invoiceautomationservice.domain.model.Conversation;
import com.invoiceautomationservice.domain.model.ConversationChannel;
import com.invoiceautomationservice.domain.model.ConversationContext;
import com.invoiceautomationservice.domain.model.ConversationFlowState;
import com.invoiceautomationservice.domain.model.InvoiceDraftStatus;
import com.invoiceautomationservice.infrastructure.adapter.out.interpretation.RuleBasedDocumentUnderstandingProvider;
import com.invoiceautomationservice.infrastructure.config.AiProperties;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ConversationEngineServiceTest {
  private Conversation conversation;
  private ConversationContextRepository contexts;
  private InvoiceDraftUseCase drafts;
  private ConversationEngineService engine;
  private AiProperties aiProperties;

  @BeforeEach
  void setUp() {
    var conversations = mock(ConversationRepository.class);
    contexts = mock(ConversationContextRepository.class);
    var messages = mock(MessageRepository.class);
    drafts = mock(InvoiceDraftUseCase.class);
    var access = mock(CompanyAccessService.class);
    conversation = Conversation.open("company-1", null, null, ConversationChannel.REST,
        "contact", Instant.parse("2026-09-23T14:00:00Z"));
    when(conversations.findByIdForUpdate(conversation.id())).thenReturn(conversation);
    when(conversations.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    when(messages.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    AtomicReference<ConversationContext> stored = new AtomicReference<>();
    when(contexts.findByConversationId(conversation.id()))
        .thenAnswer(invocation -> Optional.ofNullable(stored.get()));
    when(contexts.save(any())).thenAnswer(invocation -> {
      ConversationContext value = invocation.getArgument(0);
      stored.set(value);
      return value;
    });
    aiProperties = new AiProperties();
    engine = new ConversationEngineService(conversations, contexts, messages, drafts, access,
        new ConversationCommandParser(), new RuleBasedDocumentUnderstandingProvider(),
        aiProperties, mock(AuditTrailService.class),
        Clock.fixed(Instant.parse("2026-09-23T15:00:00Z"), ZoneOffset.UTC));
  }

  @Test
  void buildsDraftThroughExistingUseCase() {
    engine.process(conversation.id(), "NUEVA BOLETA DNI 12345678 PEN", "m1");
    engine.process(conversation.id(), "AGREGAR 2 | Servicio | 100.00", "m2");
    InvoiceDraftResponse draft = mock(InvoiceDraftResponse.class);
    var draftId = java.util.UUID.randomUUID();
    when(draft.id()).thenReturn(draftId);
    when(drafts.create(any())).thenReturn(draft);

    var result = engine.process(conversation.id(), "GENERAR", "m3");

    assertThat(result.context().state()).isEqualTo(ConversationFlowState.DRAFT_CREATED);
    assertThat(result.context().invoiceDraftId()).isEqualTo(draftId);
    verify(drafts).create(any());
  }

  @Test
  void explainsUnknownCommandWithoutMutatingFlow() {
    var result = engine.process(conversation.id(), "texto libre", "m1");
    assertThat(result.reply()).contains("No entendí").contains("NUEVA BOLETA");
    assertThat(result.context().state()).isEqualTo(ConversationFlowState.EMPTY);
  }

  @Test
  void accumulatesNaturalLanguageItemsAndCalculatesTotal() {
    var started = engine.process(conversation.id(),
        "Quiero una boleta para DNI 12345678", "n1");
    var firstItem = engine.process(conversation.id(),
        "Leche Gloria, dos unidades a 3.50", "n2");
    var secondItem = engine.process(conversation.id(),
        "Agrega tres panes a un sol", "n3");

    assertThat(started.context().state()).isEqualTo(ConversationFlowState.COLLECTING_DATA);
    assertThat(firstItem.context().itemCount()).isEqualTo(1);
    assertThat(secondItem.context().itemCount()).isEqualTo(2);
    assertThat(secondItem.reply()).contains("Total acumulado PEN 10.00");
  }

  @Test
  void keepsPartialHeaderAndRequestsMissingRecipient() {
    var partial = engine.process(conversation.id(), "Quiero una boleta", "p1");
    var completed = engine.process(conversation.id(), "DNI 12345678", "p2");

    assertThat(partial.context().state()).isEqualTo(ConversationFlowState.COLLECTING_DATA);
    assertThat(partial.context().documentType()).isNull();
    assertThat(partial.context().review().detectedValues())
        .containsEntry("documentType", "SALES_RECEIPT");
    assertThat(partial.reply()).contains("Falta indicar").contains("DNI o RUC");
    assertThat(completed.context().state()).isEqualTo(ConversationFlowState.COLLECTING_DATA);
    assertThat(completed.context().recipientDocumentNumber()).isEqualTo("12345678");
  }

  @Test
  void requiresConfirmationWhenReportedTotalDoesNotMatchBackendCalculation() {
    engine.process(conversation.id(), "Quiero una boleta para DNI 12345678", "w1");

    var result = engine.process(conversation.id(),
        "Leche Gloria, dos unidades, precio unitario 3.50 total = 8", "w2");

    assertThat(result.context().state()).isEqualTo(ConversationFlowState.NEEDS_REVIEW);
    assertThat(result.context().itemCount()).isZero();
    assertThat(result.context().review().calculationErrors())
        .singleElement().asString().contains("no coincide con el total calculado 7.00");
    assertThat(result.reply()).contains("CONFIRMAR");

    var confirmed = engine.process(conversation.id(), "CONFIRMAR", "w3");
    assertThat(confirmed.context().state()).isEqualTo(ConversationFlowState.READY_TO_CREATE);
    assertThat(confirmed.context().itemCount()).isEqualTo(1);
  }

  @Test
  void requestsConfirmationWhenConfidenceIsBelowConfiguredThreshold() {
    aiProperties.setReviewThreshold(new java.math.BigDecimal("0.99"));

    var detected = engine.process(conversation.id(),
        "Quiero una boleta para DNI 12345678", "c1");

    assertThat(detected.context().state()).isEqualTo(ConversationFlowState.NEEDS_REVIEW);
    assertThat(detected.context().review().confirmationRequired()).isTrue();
    assertThat(detected.context().review().detectedValues())
        .containsEntry("recipientDocumentNumber", "12345678");
    assertThat(detected.context().review().confirmedValues())
        .doesNotContainKey("recipientDocumentNumber");
    assertThat(detected.reply()).contains("confianza").contains("CONFIRMAR");

    var confirmed = engine.process(conversation.id(), "CONFIRMAR", "c2");
    assertThat(confirmed.context().state()).isEqualTo(ConversationFlowState.COLLECTING_DATA);
    assertThat(confirmed.context().review().confirmedValues())
        .containsEntry("recipientDocumentNumber", "12345678");
    assertThat(confirmed.context().review().confirmationRequired()).isFalse();
  }

  @Test
  void exposesAmbiguousValuesAndDoesNotAllowBlindConfirmation() {
    var ambiguous = engine.process(conversation.id(),
        "Quiero boleta y factura para DNI 12345678 y RUC 20123456789", "a1");

    assertThat(ambiguous.context().state()).isEqualTo(ConversationFlowState.NEEDS_REVIEW);
    assertThat(ambiguous.context().review().ambiguousFields())
        .containsExactly("documentType", "recipientDocument");
    assertThat(ambiguous.reply()).contains("ambiguos");

    var rejected = engine.process(conversation.id(), "CONFIRMAR", "a2");
    assertThat(rejected.context().state()).isEqualTo(ConversationFlowState.NEEDS_REVIEW);
    assertThat(rejected.reply()).contains("ambiguos");
  }

  @Test
  void completesAPendingItemThroughConversationalCorrections() {
    engine.process(conversation.id(), "Quiero una boleta para DNI 12345678", "pc1");
    var partial = engine.process(conversation.id(), "Agrega Leche Gloria", "pc2");

    assertThat(partial.context().state()).isEqualTo(ConversationFlowState.COLLECTING_DATA);
    assertThat(partial.context().review().missingFields())
        .contains("items[0].quantity", "items[0].unitPrice");

    var price = engine.process(conversation.id(), "El precio es 3.80", "pc3");
    assertThat(price.context().review().missingFields()).containsExactly("items[0].quantity");
    assertThat(price.context().review().detectedValues())
        .containsEntry("items[0].unitPrice", "3.80");

    var completed = engine.process(conversation.id(),
        "Cambia la cantidad de Leche Gloria a 2", "pc4");
    assertThat(completed.context().state()).isEqualTo(ConversationFlowState.READY_TO_CREATE);
    assertThat(completed.context().itemCount()).isEqualTo(1);
    assertThat(completed.reply()).contains("PEN 7.60");
  }

  @Test
  void correctsAndRemovesConfirmedContextData() {
    engine.process(conversation.id(), "Quiero una boleta para DNI 12345678", "cc1");
    engine.process(conversation.id(), "Leche Gloria, dos unidades a 3.50", "cc2");

    var price = engine.process(conversation.id(), "El precio es 3.80", "cc3");
    assertThat(price.reply()).contains("PEN 7.60");

    var quantity = engine.process(conversation.id(),
        "Cambia la cantidad de Leche Gloria a 3", "cc4");
    assertThat(quantity.reply()).contains("PEN 11.40");

    engine.process(conversation.id(), "Agrega tres panes a un sol", "cc5");
    var removed = engine.process(conversation.id(), "Elimina el pan", "cc6");
    assertThat(removed.context().itemCount()).isEqualTo(1);

    var recipient = engine.process(conversation.id(),
        "El DNI correcto es 87654321", "cc7");
    assertThat(recipient.context().recipientDocumentNumber()).isEqualTo("87654321");

    var invoice = engine.process(conversation.id(), "Es factura, no boleta", "cc8");
    assertThat(invoice.context().state()).isEqualTo(ConversationFlowState.COLLECTING_DATA);
    assertThat(invoice.context().documentType())
        .isEqualTo(com.invoiceautomationservice.domain.model.InvoiceDocumentType.INVOICE);
    assertThat(invoice.context().recipientDocumentNumber()).isNull();
    assertThat(invoice.reply()).contains("RUC");

    var ruc = engine.process(conversation.id(),
        "El RUC correcto es 20123456789", "cc9");
    assertThat(ruc.context().state()).isEqualTo(ConversationFlowState.READY_TO_CREATE);
    assertThat(ruc.context().recipientDocumentNumber()).isEqualTo("20123456789");
  }

  @Test
  void requestsAFullNameWhenAnItemReferenceIsAmbiguous() {
    engine.process(conversation.id(), "NUEVA BOLETA DNI 12345678 PEN", "ac1");
    engine.process(conversation.id(), "AGREGAR 1 | Pan blanco | 1.00", "ac2");
    engine.process(conversation.id(), "AGREGAR 1 | Pan integral | 1.50", "ac3");

    var result = engine.process(conversation.id(), "Elimina el pan", "ac4");

    assertThat(result.context().itemCount()).isEqualTo(2);
    assertThat(result.reply()).contains("varios productos");
  }

  @Test
  void editsExistingDraftThroughInvoiceDraftUseCase() {
    engine.process(conversation.id(), "NUEVA BOLETA DNI 12345678 PEN", "dc1");
    engine.process(conversation.id(), "AGREGAR 2 | Servicio | 100.00", "dc2");
    var draftId = java.util.UUID.randomUUID();
    InvoiceDraftResponse created = mock(InvoiceDraftResponse.class);
    when(created.id()).thenReturn(draftId);
    when(drafts.create(any())).thenReturn(created);
    engine.process(conversation.id(), "GENERAR", "dc3");

    InvoiceItemResponse originalItem = new InvoiceItemResponse(java.util.UUID.randomUUID(),
        "Servicio", new java.math.BigDecimal("2"), new java.math.BigDecimal("100.00"),
        new java.math.BigDecimal("200.00"));
    InvoiceDraftResponse original = draftResponse(draftId, originalItem,
        new java.math.BigDecimal("200.00"), InvoiceDraftStatus.DRAFT);
    InvoiceItemResponse correctedItem = new InvoiceItemResponse(originalItem.id(),
        "Servicio", new java.math.BigDecimal("3"), new java.math.BigDecimal("100.00"),
        new java.math.BigDecimal("300.00"));
    InvoiceDraftResponse corrected = draftResponse(draftId, correctedItem,
        new java.math.BigDecimal("300.00"), InvoiceDraftStatus.DRAFT);
    when(drafts.findById(draftId)).thenReturn(original);
    when(drafts.update(org.mockito.ArgumentMatchers.eq(draftId), any())).thenReturn(corrected);

    var result = engine.process(conversation.id(),
        "Cambia la cantidad de Servicio a 3", "dc4");

    var request = org.mockito.ArgumentCaptor.forClass(UpdateInvoiceDraftRequest.class);
    verify(drafts).update(org.mockito.ArgumentMatchers.eq(draftId), request.capture());
    assertThat(request.getValue().items().getFirst().quantity()).isEqualByComparingTo("3");
    assertThat(result.context().state()).isEqualTo(ConversationFlowState.DRAFT_CREATED);
    assertThat(result.reply()).contains("Borrador corregido").contains("PEN 300.00");
  }

  @Test
  void doesNotEditDraftThatIsNoLongerInDraftState() {
    engine.process(conversation.id(), "NUEVA BOLETA DNI 12345678 PEN", "dr1");
    engine.process(conversation.id(), "AGREGAR 2 | Servicio | 100.00", "dr2");
    var draftId = java.util.UUID.randomUUID();
    InvoiceDraftResponse created = mock(InvoiceDraftResponse.class);
    when(created.id()).thenReturn(draftId);
    when(drafts.create(any())).thenReturn(created);
    engine.process(conversation.id(), "GENERAR", "dr3");
    when(drafts.findById(draftId)).thenReturn(draftResponse(draftId,
        new InvoiceItemResponse(java.util.UUID.randomUUID(), "Servicio",
            java.math.BigDecimal.ONE, java.math.BigDecimal.TEN, java.math.BigDecimal.TEN),
        java.math.BigDecimal.TEN, InvoiceDraftStatus.APPROVED));

    var result = engine.process(conversation.id(),
        "Cambia la cantidad de Servicio a 3", "dr4");

    assertThat(result.reply()).contains("ya no está en estado DRAFT");
    verify(drafts, never()).update(any(), any());
  }

  private InvoiceDraftResponse draftResponse(java.util.UUID id, InvoiceItemResponse item,
      java.math.BigDecimal total, InvoiceDraftStatus status) {
    Instant now = Instant.parse("2026-09-23T15:00:00Z");
    return new InvoiceDraftResponse(id, "company-1", "customer-1",
        com.invoiceautomationservice.domain.model.InvoiceDocumentType.SALES_RECEIPT,
        com.invoiceautomationservice.domain.model.IdentityDocumentType.DNI, "12345678", "PEN",
        status, List.of(item), total, total, now, now, null, null);
  }
}
