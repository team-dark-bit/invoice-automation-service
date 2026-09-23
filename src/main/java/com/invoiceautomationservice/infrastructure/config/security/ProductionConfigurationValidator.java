package com.invoiceautomationservice.infrastructure.config.security;

import java.util.Arrays;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class ProductionConfigurationValidator implements InitializingBean {
  private static final String LOCAL_JWT_SECRET = "local-development-jwt-secret-change-me";
  private static final String LOCAL_DB_PASSWORD = "invoice_password";
  private final Environment environment;

  public ProductionConfigurationValidator(Environment environment) {
    this.environment = environment;
  }

  @Override
  public void afterPropertiesSet() {
    if (Arrays.stream(environment.getActiveProfiles()).noneMatch("prod"::equals)) return;
    String jwtSecret = required("jwt.secret");
    if (jwtSecret.length() < 32 || LOCAL_JWT_SECRET.equals(jwtSecret)
        || "default_jwt_secret".equals(jwtSecret)) {
      throw new IllegalStateException(
          "JWT_SECRET must be a non-default value with at least 32 characters in prod");
    }
    String databasePassword = required("spring.datasource.password");
    if (databasePassword.length() < 12 || LOCAL_DB_PASSWORD.equals(databasePassword)) {
      throw new IllegalStateException(
          "DB_PASSWORD must be a non-default value with at least 12 characters in prod");
    }
    required("billing.provider");
  }

  private String required(String property) {
    String value = environment.getProperty(property);
    if (value == null || value.isBlank()) {
      throw new IllegalStateException(property + " is required in prod");
    }
    return value;
  }
}
