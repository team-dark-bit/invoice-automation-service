package com.invoiceautomationservice.infrastructure.config.observability;

import org.flywaydb.core.Flyway;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

@Component("flywayMigrationHealthIndicator")
public class FlywayHealthIndicator implements HealthIndicator {
  private final Flyway flyway;

  public FlywayHealthIndicator(Flyway flyway) {
    this.flyway = flyway;
  }

  @Override
  public Health health() {
    var pending = flyway.info().pending();
    var current = flyway.info().current();
    if (pending.length > 0) {
      return Health.down().withDetail("pendingMigrations", pending.length)
          .withDetail("currentVersion", current == null ? "none" : current.getVersion()).build();
    }
    return Health.up()
        .withDetail("currentVersion", current == null ? "none" : current.getVersion())
        .withDetail("pendingMigrations", 0).build();
  }
}
