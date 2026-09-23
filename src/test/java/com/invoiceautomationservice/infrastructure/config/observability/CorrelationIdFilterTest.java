package com.invoiceautomationservice.infrastructure.config.observability;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class CorrelationIdFilterTest {
  private final CorrelationIdFilter filter = new CorrelationIdFilter();

  @Test
  void propagatesSafeCorrelationIdAndClearsMdc() throws Exception {
    var request = new MockHttpServletRequest();
    request.addHeader(CorrelationIdFilter.HEADER, "request-123");
    var response = new MockHttpServletResponse();
    FilterChain chain = (req, res) -> assertThat(MDC.get("correlationId"))
        .isEqualTo("request-123");

    filter.doFilter(request, response, chain);

    assertThat(response.getHeader(CorrelationIdFilter.HEADER)).isEqualTo("request-123");
    assertThat(MDC.get("correlationId")).isNull();
  }

  @Test
  void replacesUnsafeCorrelationId() throws Exception {
    var request = new MockHttpServletRequest();
    request.addHeader(CorrelationIdFilter.HEADER, "bad value with spaces\nforged");
    var response = new MockHttpServletResponse();

    filter.doFilter(request, response, (req, res) -> {});

    assertThat(response.getHeader(CorrelationIdFilter.HEADER))
        .matches("[0-9a-f-]{36}").doesNotContain("forged");
  }
}
