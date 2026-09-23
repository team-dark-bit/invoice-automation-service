package com.invoiceautomationservice.infrastructure.config.security;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class ProductionConfigurationValidatorTest {
  @Test
  void acceptsLocalProfileDefaults() {
    var environment = new MockEnvironment()
        .withProperty("jwt.secret", "local-development-jwt-secret-change-me")
        .withProperty("spring.datasource.password", "invoice_password");
    assertThatCode(() -> new ProductionConfigurationValidator(environment).afterPropertiesSet())
        .doesNotThrowAnyException();
  }

  @Test
  void rejectsDefaultJwtSecretInProduction() {
    var environment = validProductionEnvironment()
        .withProperty("jwt.secret", "default_jwt_secret");
    assertThatThrownBy(() -> new ProductionConfigurationValidator(environment).afterPropertiesSet())
        .isInstanceOf(IllegalStateException.class).hasMessageContaining("JWT_SECRET");
  }

  @Test
  void acceptsStrongProductionSecrets() {
    assertThatCode(() -> new ProductionConfigurationValidator(
        validProductionEnvironment()).afterPropertiesSet()).doesNotThrowAnyException();
  }

  private MockEnvironment validProductionEnvironment() {
    return new MockEnvironment().withProperty("spring.profiles.active", "prod")
        .withProperty("jwt.secret", "a-production-secret-with-more-than-32-characters")
        .withProperty("spring.datasource.password", "strong-db-password")
        .withProperty("billing.provider", "nubefact");
  }
}
