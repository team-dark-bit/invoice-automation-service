package com.invoiceautomationservice.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.invoiceautomationservice.application.port.out.CompanyRepository;
import com.invoiceautomationservice.application.port.out.CustomerRepository;
import com.invoiceautomationservice.application.port.out.DocumentSeriesRepository;
import com.invoiceautomationservice.application.port.out.InvoiceDraftRepository;
import com.invoiceautomationservice.application.port.out.IssuerTaxProfileRepository;
import com.invoiceautomationservice.application.service.CompanyAccessService;
import com.invoiceautomationservice.application.service.InvoiceIssuancePersistenceService;
import com.invoiceautomationservice.application.service.PreparedEmission;
import com.invoiceautomationservice.domain.model.Company;
import com.invoiceautomationservice.domain.model.Customer;
import com.invoiceautomationservice.domain.model.DocumentNumber;
import com.invoiceautomationservice.domain.model.DocumentSeries;
import com.invoiceautomationservice.domain.model.IdentityDocumentType;
import com.invoiceautomationservice.domain.model.InvoiceDocumentType;
import com.invoiceautomationservice.domain.model.InvoiceDraft;
import com.invoiceautomationservice.domain.model.InvoiceItem;
import com.invoiceautomationservice.domain.model.IssuerTaxProfile;
import com.invoiceautomationservice.domain.model.TaxpayerType;
import com.invoiceautomationservice.infrastructure.config.exception.ApplicationException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers(disabledWithoutDocker = true)
class PostgreSqlConcurrencyIT {
  @Container
  static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>("postgres:17-alpine")
          .withDatabaseName("invoice_integration")
          .withUsername("invoice_test")
          .withPassword("invoice_test");

  @DynamicPropertySource
  static void databaseProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
    registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
  }

  @Autowired Flyway flyway;
  @Autowired JdbcTemplate jdbc;
  @Autowired DocumentSeriesRepository seriesRepository;
  @Autowired CompanyAccessService accessService;
  @Autowired CompanyRepository companyRepository;
  @Autowired CustomerRepository customerRepository;
  @Autowired IssuerTaxProfileRepository profileRepository;
  @Autowired InvoiceDraftRepository draftRepository;
  @Autowired InvoiceIssuancePersistenceService issuanceService;
  @Autowired PlatformTransactionManager transactionManager;

  @Test
  void appliesEveryFlywayMigrationAndValidatesJpaSchema() {
    var applied = flyway.info().applied();
    assertThat(applied).isNotEmpty();
    assertThat(applied[applied.length - 1].getVersion().getVersion()).isEqualTo("22");
    assertThat(jdbc.queryForObject(
        "select count(*) from information_schema.tables where table_name in "
            + "('electronic_documents','audit_events','conversations','messages',"
            + "'conversation_contexts')", Integer.class)).isEqualTo(5);
  }

  @Test
  void isolatesCompanyMembershipsUsingRealPostgreSqlData() {
    String ownCompany = insertCompany("own");
    String foreignCompany = insertCompany("foreign");
    jdbc.update("insert into user_companies(user_id, company_id, created_at, role, active) "
            + "select id, ?, current_timestamp, 'OWNER', true from users where username='haroldqc'",
        ownCompany);
    authenticate("haroldqc");

    accessService.requireAccess(ownCompany);
    assertThatThrownBy(() -> accessService.requireAccess(foreignCompany))
        .isInstanceOf(ApplicationException.class);
  }

  @Test
  void reservesUniqueGaplessCorrelativesUnderConcurrency() throws Exception {
    String companyId = insertCompany("series");
    seriesRepository.save(new DocumentSeries(UUID.randomUUID().toString(), companyId,
        InvoiceDocumentType.SALES_RECEIPT, "B001", 0, true));
    int workers = 20;
    CountDownLatch ready = new CountDownLatch(workers);
    CountDownLatch start = new CountDownLatch(1);
    var executor = Executors.newFixedThreadPool(workers);
    TransactionTemplate transactions = new TransactionTemplate(transactionManager);
    List<Future<DocumentNumber>> futures = new ArrayList<>();
    try {
      for (int index = 0; index < workers; index++) {
        futures.add(executor.submit(() -> {
          ready.countDown();
          start.await();
          return transactions.execute(status -> seriesRepository.reserveNext(
              companyId, InvoiceDocumentType.SALES_RECEIPT));
        }));
      }
      ready.await();
      start.countDown();
      List<Long> values = new ArrayList<>();
      for (Future<DocumentNumber> future : futures) values.add(future.get().correlative());

      assertThat(new HashSet<>(values)).hasSize(workers);
      assertThat(values).containsExactlyInAnyOrderElementsOf(
          java.util.stream.LongStream.rangeClosed(1, workers).boxed().toList());
      assertThat(jdbc.queryForObject(
          "select current_correlative from document_series where company_id = ?",
          Long.class, companyId)).isEqualTo((long) workers);
    } finally {
      executor.shutdownNow();
    }
  }

  @Test
  void preparesOneElectronicDocumentForConcurrentDuplicateEmission() throws Exception {
    String companyId = insertCompany("issue");
    jdbc.update("insert into user_companies(user_id, company_id, created_at, role, active) "
            + "select id, ?, current_timestamp, 'OWNER', true from users where username='haroldqc'",
        companyId);
    Customer customer = new Customer();
    customer.setId(UUID.randomUUID().toString());
    customer.setCompanyId(companyId);
    customer.setFullName("Integration Customer");
    customer.setDocumentType("DNI");
    customer.setDocumentNumber(uniqueDigits(8));
    customer.setActive(true);
    customer = customerRepository.save(customer);
    Instant now = Instant.parse("2026-09-23T15:00:00Z");
    profileRepository.save(new IssuerTaxProfile(companyId, TaxpayerType.LEGAL_ENTITY,
        "Lima", "150101", "Lima", "Lima", "Lima", "PE", now, now));
    seriesRepository.save(new DocumentSeries(UUID.randomUUID().toString(), companyId,
        InvoiceDocumentType.SALES_RECEIPT, "B001", 0, true));
    InvoiceDraft draft = InvoiceDraft.create(companyId, customer.getId(),
        InvoiceDocumentType.SALES_RECEIPT, IdentityDocumentType.DNI,
        customer.getDocumentNumber(), "PEN",
        List.of(InvoiceItem.create("Service", BigDecimal.ONE, BigDecimal.TEN)), now)
        .approve(now.plusSeconds(1));
    draft = draftRepository.save(draft);
    UUID draftId = draft.id();
    CountDownLatch start = new CountDownLatch(1);
    var executor = Executors.newFixedThreadPool(2);
    try {
      var task = (java.util.concurrent.Callable<PreparedEmission>) () -> {
        authenticate("haroldqc");
        start.await();
        return issuanceService.prepare(draftId);
      };
      Future<PreparedEmission> first = executor.submit(task);
      Future<PreparedEmission> second = executor.submit(task);
      start.countDown();
      PreparedEmission one = first.get();
      PreparedEmission two = second.get();

      assertThat(one.document().id()).isEqualTo(two.document().id());
      assertThat(List.of(one.submitRequired(), two.submitRequired()))
          .containsExactlyInAnyOrder(true, false);
      assertThat(jdbc.queryForObject(
          "select count(*) from electronic_documents where draft_id = ?", Integer.class, draftId))
          .isEqualTo(1);
      assertThat(jdbc.queryForObject(
          "select current_correlative from document_series where company_id = ?",
          Long.class, companyId)).isEqualTo(1L);
    } finally {
      executor.shutdownNow();
    }
  }

  private String insertCompany(String suffix) {
    Company company = new Company();
    company.setId(UUID.randomUUID().toString());
    company.setLegalName("Integration " + suffix);
    company.setTradeName("IT " + suffix);
    company.setTaxId(uniqueDigits(11));
    company.setAddress("Lima");
    company.setActive(true);
    return companyRepository.save(company).getId();
  }

  private String uniqueDigits(int length) {
    String digits = Long.toUnsignedString(Math.abs(UUID.randomUUID().getMostSignificantBits()));
    return (digits + "00000000000000000000").substring(0, length);
  }

  private void authenticate(String username) {
    SecurityContextHolder.getContext().setAuthentication(
        UsernamePasswordAuthenticationToken.authenticated(username, "n/a", List.of()));
  }
}
