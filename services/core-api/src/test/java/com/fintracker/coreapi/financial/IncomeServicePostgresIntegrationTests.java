package com.fintracker.coreapi.financial;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fintracker.coreapi.financial.dto.request.IncomeRequest;
import com.fintracker.coreapi.financial.dto.response.IncomeResponse;
import com.fintracker.coreapi.financial.entity.Income;
import com.fintracker.coreapi.financial.exception.IncomeNotFoundException;
import com.fintracker.coreapi.financial.repository.IncomeRepository;
import com.fintracker.coreapi.financial.service.IncomeService;
import com.fintracker.coreapi.identity.entity.AppUser;
import com.fintracker.coreapi.identity.repository.AppUserRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * PostgreSQL integration tests for income use cases.
 * These tests verify persistence, generated audit timestamps, authenticated-user scoping for collections, and rejection of another user's IDs during read, update, and delete.
 * A real PostgreSQL Testcontainer is used to validate the schema and repository predicates together.
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers

class IncomeServicePostgresIntegrationTests {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("fintracker_income_service_test")
            .withUsername("fintracker_test")
            .withPassword("test_password");

/**
     * Registers the Testcontainers PostgreSQL connection properties and enables Flyway/schema validation for the Spring test context.
     */
    @DynamicPropertySource

    static void configureDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.flyway.enabled", () -> true);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }

    @Autowired
    private IncomeService incomeService;

    @Autowired
    private IncomeRepository incomeRepository;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

/**
     * Clears authentication and resets persisted test users before each test so records from a prior scenario cannot affect the next one.
     */
    @BeforeEach

    void cleanDatabase() {
        SecurityContextHolder.clearContext();
        jdbcTemplate.update("DELETE FROM app_users");
    }

/**
     * Clears Spring Security's thread-local context after the test to prevent authentication leaking into another test.
     */
    @AfterEach

    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

/**
     * Verifies that create income for authenticated user under the PostgreSQL-backed integration scenario, including the relevant persistence and tenant-isolation expectations.
     */
    @Test

    void shouldCreateIncomeForAuthenticatedUser() {
        AppUser user = createUser("income.create@example.com");

        authenticateAs(user);

        IncomeResponse response = incomeService.create(request("Salary", "5000.00", "USD", LocalDate.of(2026, 1, 1)));

        assertThat(response.id()).isNotNull();
        assertThat(response.source()).isEqualTo("Salary");
        assertThat(response.amount()).isEqualByComparingTo("5000.00");
        assertThat(response.currency()).isEqualTo("USD");
        assertThat(response.recurring()).isFalse();

        Income persisted =
                incomeRepository.findByIdAndUserId(response.id(), user.getId()).orElseThrow();

        assertThat(persisted.getUserId()).isEqualTo(user.getId());
        assertThat(persisted.getCreatedAt()).isNotNull();
        assertThat(persisted.getUpdatedAt()).isNotNull();
    }

/**
     * Verifies that list only authenticated users income under the PostgreSQL-backed integration scenario, including the relevant persistence and tenant-isolation expectations.
     */
    @Test

    void shouldListOnlyAuthenticatedUsersIncome() {
        AppUser userA = createUser("income.list.a@example.com");
        AppUser userB = createUser("income.list.b@example.com");

        authenticateAs(userA);
        incomeService.create(request("Salary", "5000.00", "USD", LocalDate.of(2026, 1, 1)));
        incomeService.create(request("Freelance", "750.00", "USD", LocalDate.of(2026, 1, 5)));

        authenticateAs(userB);
        incomeService.create(request("Consulting", "2500.00", "USD", LocalDate.of(2026, 1, 3)));

        authenticateAs(userA);
        Page<IncomeResponse> results = incomeService.getAll(PageRequest.of(0, 20));

        assertThat(results.getContent())
                .hasSize(2)
                .extracting(IncomeResponse::source)
                .containsExactlyInAnyOrder("Salary", "Freelance");

        assertThat(results.getTotalElements()).isEqualTo(2);
    }

/**
     * Verifies that return owned income but hide another users income under the PostgreSQL-backed integration scenario, including the relevant persistence and tenant-isolation expectations.
     */
    @Test

    void shouldReturnOwnedIncomeButHideAnotherUsersIncome() {
        AppUser userA = createUser("income.get.a@example.com");
        AppUser userB = createUser("income.get.b@example.com");

        authenticateAs(userA);
        IncomeResponse created = incomeService.create(request("Salary", "5000.00", "USD", LocalDate.of(2026, 1, 1)));

        assertThat(incomeService.getById(created.id()).id()).isEqualTo(created.id());

        authenticateAs(userB);

        assertThatThrownBy(() -> incomeService.getById(created.id())).isInstanceOf(IncomeNotFoundException.class);
    }

/**
     * Verifies that update owned income and reject another users income under the PostgreSQL-backed integration scenario, including the relevant persistence and tenant-isolation expectations.
     */
    @Test

    void shouldUpdateOwnedIncomeAndRejectAnotherUsersIncome() {
        AppUser userA = createUser("income.update.a@example.com");
        AppUser userB = createUser("income.update.b@example.com");

        authenticateAs(userA);
        IncomeResponse created = incomeService.create(request("Salary", "5000.00", "USD", LocalDate.of(2026, 1, 1)));

        IncomeResponse updated =
                incomeService.update(created.id(), request("New salary", "6200.00", "EUR", LocalDate.of(2026, 2, 1)));

        assertThat(updated.id()).isEqualTo(created.id());
        assertThat(updated.source()).isEqualTo("New salary");
        assertThat(updated.amount()).isEqualByComparingTo("6200.00");
        assertThat(updated.currency()).isEqualTo("EUR");
        assertThat(updated.date()).isEqualTo(LocalDate.of(2026, 2, 1));

        authenticateAs(userB);

        assertThatThrownBy(() -> incomeService.update(
                        created.id(), request("Unauthorized change", "1.00", "USD", LocalDate.of(2026, 3, 1))))
                .isInstanceOf(IncomeNotFoundException.class);

        Income persisted =
                incomeRepository.findByIdAndUserId(created.id(), userA.getId()).orElseThrow();

        assertThat(persisted.getSource()).isEqualTo("New salary");
        assertThat(persisted.getAmount()).isEqualByComparingTo("6200.00");
    }

/**
     * Verifies that delete owned income and reject another users delete under the PostgreSQL-backed integration scenario, including the relevant persistence and tenant-isolation expectations.
     */
    @Test

    void shouldDeleteOwnedIncomeAndRejectAnotherUsersDelete() {
        AppUser userA = createUser("income.delete.a@example.com");
        AppUser userB = createUser("income.delete.b@example.com");

        authenticateAs(userA);
        IncomeResponse created = incomeService.create(request("Salary", "5000.00", "USD", LocalDate.of(2026, 1, 1)));

        authenticateAs(userB);

        assertThatThrownBy(() -> incomeService.delete(created.id())).isInstanceOf(IncomeNotFoundException.class);

        assertThat(incomeRepository.existsById(created.id())).isTrue();

        authenticateAs(userA);
        incomeService.delete(created.id());

        assertThat(incomeRepository.existsById(created.id())).isFalse();
    }

    /**
     * Persists a uniquely identified application user used as the owner fixture for this integration test.
     */
    private AppUser createUser(String email) {
        return appUserRepository.save(new AppUser(UUID.randomUUID(), email, null));
    }

    /**
     * Installs a test authentication whose JWT subject identifies the supplied user, allowing the service to resolve tenant ownership exactly as it does in a request.
     */
    private void authenticateAs(AppUser user) {
        Instant now = Instant.now();

        Jwt jwt = Jwt.withTokenValue("integration-test-token")
                .header("alg", "HS256")
                .subject(user.getId().toString())
                .issuedAt(now)
                .expiresAt(now.plusSeconds(300))
                .claim("token_use", "access")
                .build();

        SecurityContextHolder.getContext()
                .setAuthentication(new TestingAuthenticationToken(jwt, "integration-test-credentials", "ROLE_USER"));
    }

    /**
     * Builds a representative validated income request from the supplied test values.
     */
    private IncomeRequest request(String source, String amount, String currency, LocalDate date) {

        return new IncomeRequest(
                source,
                new BigDecimal(amount),
                currency,
                "Employment",
                "Integration test income",
                date,
                false,
                null,
                null,
                null);
    }
}
