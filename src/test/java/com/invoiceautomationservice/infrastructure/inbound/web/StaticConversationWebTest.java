package com.invoiceautomationservice.infrastructure.inbound.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class StaticConversationWebTest {

  @Test
  void packagesTheConversationWebAndItsMainApiFlows() throws IOException {
    String html = resource("/static/index.html");
    String javascript = resource("/static/app.js");
    String stylesheet = resource("/static/styles.css");

    assertThat(html)
        .contains("id=\"login-form\"")
        .contains("id=\"company-select\"")
        .contains("id=\"messages\"")
        .contains("id=\"image-input\"")
        .contains("id=\"summary-content\"")
        .contains("id=\"approve-draft\"")
        .contains("id=\"issue-draft\"")
        .contains("src=\"/app.js\"")
        .contains("href=\"/styles.css\"");
    assertThat(javascript)
        .contains("/api/auth/login")
        .contains("/api/v1/companies")
        .contains("/api/v1/conversations")
        .contains("/images")
        .contains("/approve")
        .contains("/issue")
        .contains("AWAITING_DRAFT_CONFIRMATION");
    assertThat(stylesheet)
        .contains(".workspace-grid")
        .contains(".processing-banner")
        .contains("@media (max-width: 580px)");
  }

  private String resource(String path) throws IOException {
    try (var stream = getClass().getResourceAsStream(path)) {
      assertThat(stream).as("classpath resource %s", path).isNotNull();
      return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
    }
  }
}
