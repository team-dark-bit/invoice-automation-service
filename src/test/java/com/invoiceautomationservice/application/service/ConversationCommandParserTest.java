package com.invoiceautomationservice.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.invoiceautomationservice.domain.model.IdentityDocumentType;
import com.invoiceautomationservice.domain.model.InvoiceDocumentType;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class ConversationCommandParserTest {
  private final ConversationCommandParser parser = new ConversationCommandParser();

  @Test
  void parsesInvoiceStartCommandCaseInsensitively() {
    var command = (ConversationCommandParser.StartCommand)
        parser.parse(" nueva   factura ruc 20123456789 pen ");
    assertThat(command.documentType()).isEqualTo(InvoiceDocumentType.INVOICE);
    assertThat(command.identityType()).isEqualTo(IdentityDocumentType.RUC);
    assertThat(command.currency()).isEqualTo("PEN");
  }

  @Test
  void parsesItemUsingChannelNeutralTextFormat() {
    var command = (ConversationCommandParser.AddItemCommand)
        parser.parse("AGREGAR 2 | Servicio mensual | 100.50");
    assertThat(command.description()).isEqualTo("Servicio mensual");
    assertThat(command.quantity()).isEqualByComparingTo(new BigDecimal("2"));
    assertThat(command.unitPrice()).isEqualByComparingTo(new BigDecimal("100.50"));
  }

  @Test
  void returnsUnknownForMalformedCommand() {
    assertThat(parser.parse("crear algo"))
        .isInstanceOf(ConversationCommandParser.UnknownCommand.class);
  }
}
