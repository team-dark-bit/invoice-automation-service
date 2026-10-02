package com.invoiceautomationservice.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.invoiceautomationservice.application.dto.response.InvoiceDraftResponse;
import com.invoiceautomationservice.application.port.in.InvoiceDraftUseCase;
import com.invoiceautomationservice.application.port.out.ConversationContextRepository;
import com.invoiceautomationservice.application.port.out.ConversationRepository;
import com.invoiceautomationservice.application.port.out.MessageRepository;
import com.invoiceautomationservice.domain.model.CompanyPermission;
import com.invoiceautomationservice.domain.model.Conversation;
import com.invoiceautomationservice.domain.model.ConversationChannel;
import com.invoiceautomationservice.domain.model.ConversationContext;
import com.invoiceautomationservice.domain.model.ConversationFlowState;
import com.invoiceautomationservice.infrastructure.adapter.out.interpretation.RuleBasedDocumentUnderstandingProvider;
import com.invoiceautomationservice.infrastructure.config.AiProperties;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
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
}
