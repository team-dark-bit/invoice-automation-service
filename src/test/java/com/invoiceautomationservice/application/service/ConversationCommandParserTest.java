package com.invoiceautomationservice.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.invoiceautomationservice.domain.model.IdentityDocumentType;
import com.invoiceautomationservice.domain.model.InvoiceDocumentType;
import org.junit.jupiter.api.Test;

class ConversationCommandParserTest {
  private final ConversationCommandParser parser = new ConversationCommandParser();

  @Test
  void parsesConversationalCorrections() {
    assertThat(parser.parse("Cambia la cantidad de Leche Gloria a 3"))
        .isEqualTo(new ConversationCommandParser.ChangeItemQuantityCommand(
            "Leche Gloria", new java.math.BigDecimal("3")));
    assertThat(parser.parse("El precio es 3.80"))
        .isEqualTo(new ConversationCommandParser.ChangeItemPriceCommand(
            null, new java.math.BigDecimal("3.80")));
    assertThat(parser.parse("Elimina el pan"))
        .isEqualTo(new ConversationCommandParser.RemoveItemCommand("pan"));
    assertThat(parser.parse("El DNI correcto es 87654321"))
        .isEqualTo(new ConversationCommandParser.CorrectRecipientCommand(
            IdentityDocumentType.DNI, "87654321"));
    assertThat(parser.parse("Es factura, no boleta"))
        .isEqualTo(new ConversationCommandParser.CorrectDocumentTypeCommand(
            InvoiceDocumentType.INVOICE));
  }
}
