package com.invoiceautomationservice.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.invoiceautomationservice.application.dto.request.CreateConversationRequest;
import com.invoiceautomationservice.application.dto.request.CreateMessageRequest;
import com.invoiceautomationservice.application.port.out.ConversationRepository;
import com.invoiceautomationservice.application.port.out.CustomerRepository;
import com.invoiceautomationservice.application.port.out.InvoiceDraftRepository;
import com.invoiceautomationservice.application.port.out.MessageRepository;
import com.invoiceautomationservice.domain.model.CompanyPermission;
import com.invoiceautomationservice.domain.model.Conversation;
import com.invoiceautomationservice.domain.model.ConversationChannel;
import com.invoiceautomationservice.domain.model.Message;
import com.invoiceautomationservice.domain.model.MessageDirection;
import com.invoiceautomationservice.domain.model.MessageStatus;
import com.invoiceautomationservice.domain.model.MessageType;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ConversationServiceTest {
  private ConversationRepository conversations;
  private MessageRepository messages;
  private CompanyAccessService access;
  private ConversationService service;

  @BeforeEach
  void setUp() {
    conversations = mock(ConversationRepository.class);
    messages = mock(MessageRepository.class);
    access = mock(CompanyAccessService.class);
    service = new ConversationService(conversations, messages, mock(CustomerRepository.class),
        mock(InvoiceDraftRepository.class), access, mock(AuditTrailService.class),
        Clock.fixed(Instant.parse("2026-09-23T15:00:00Z"), ZoneOffset.UTC));
  }

  @Test
  void createsConversationInsideResolvedCompany() {
    when(access.resolveCompanyId("company-1")).thenReturn("company-1");
    when(conversations.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    var response = service.create("company-1", new CreateConversationRequest(
        null, null, ConversationChannel.REST, "contact-1"));

    assertThat(response.companyId()).isEqualTo("company-1");
    assertThat(response.externalParticipantId()).isEqualTo("contact-1");
    verify(access).requirePermission("company-1", CompanyPermission.CONVERSATION_MANAGE);
  }

  @Test
  void addsMessageAndTouchesConversationAtomically() {
    Conversation conversation = Conversation.open("company-1", null, null,
        ConversationChannel.REST, "contact-1", Instant.parse("2026-09-23T14:00:00Z"));
    when(conversations.findByIdForUpdate(conversation.id())).thenReturn(conversation);
    when(messages.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    when(conversations.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    var response = service.addMessage(conversation.id(), new CreateMessageRequest(
        MessageDirection.INBOUND, MessageType.TEXT, "hola", null, "external-1",
        MessageStatus.RECEIVED));

    assertThat(response.content()).isEqualTo("hola");
    verify(messages).save(any(Message.class));
    verify(access).requirePermission("company-1", CompanyPermission.CONVERSATION_MANAGE);
  }
}
