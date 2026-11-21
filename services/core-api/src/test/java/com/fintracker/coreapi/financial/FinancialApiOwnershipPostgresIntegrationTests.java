package com.fintracker.coreapi.financial;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.server.ResponseStatusException;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * PostgreSQL integration tests for the international-transfer, loan, and loan-payment service layer.
 * Testcontainers runs the real PostgreSQL schema and Flyway migrations so these tests exercise actual persistence constraints rather than an in-memory substitute.
 * The scenarios switch JWT subjects between two users to prove that record IDs and nested loan/payment paths cannot cross tenant boundaries.
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers

class FinancialApiOwnershipPostgresIntegrationTests {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("fintracker_financial_api_test")
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
    AppUserRepository users;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    TransferApiService transfers;

    @Autowired
    LoanApiService loans;

    @Autowired
    LoanPaymentApiService payments;

    @BeforeEach
    void clean() {
        SecurityContextHolder.clearContext();
        jdbc.update("DELETE FROM app_users");
    }

    @AfterEach
    void clearAuth() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void transferCrudIsScopedToAuthenticatedOwner() {
        AppUser owner = user("transfer.owner@example.com");
        AppUser other = user("transfer.other@example.com");

        authenticate(owner);
        TransferView created = transfers.create(transferRequest("USD", "INR"));
        assertThat(created.id()).isNotNull();
        assertThat(created.currencySent()).isEqualTo("USD");
        assertThat(created.currencyReceived()).isEqualTo("INR");
        assertThat(transfers
                        .all(org.springframework.data.domain.PageRequest.of(0, 20))
                        .getTotalElements())
                .isEqualTo(1);


        authenticate(other);
        assertThatThrownBy(() -> transfers.one(created.id())).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> transfers.update(created.id(), transferRequest("EUR", "GBP")))
                .isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> transfers.delete(created.id())).isInstanceOf(ResponseStatusException.class);


        authenticate(owner);
        transfers.delete(created.id());
        assertThat(transfers
                        .all(org.springframework.data.domain.PageRequest.of(0, 20))
                        .getTotalElements())
                .isZero();
    }

    @Test
    void loanCrudIsScopedToAuthenticatedOwner() {
        AppUser owner = user("loan.owner@example.com");
        AppUser other = user("loan.other@example.com");

        authenticate(owner);
        LoanView created = loans.create(loanRequest());
        assertThat(created.currentBalance()).isEqualByComparingTo("900.00");
        LoanView updated = loans.update(
                created.id(),
                new LoanRequest(
                        "Updated personal loan",
                        new BigDecimal("1000.00"),
                        new BigDecimal("850.00"),
                        new BigDecimal("7.50"),
                        new BigDecimal("100.00"),
                        LocalDate.of(2026, 1, 1),
                        LocalDate.of(2027, 1, 1),
                        "Bank",
                        "personal",
                        "active",
                        "INR"));
        assertThat(updated.loanName()).isEqualTo("Updated personal loan");


        authenticate(other);
        assertThatThrownBy(() -> loans.one(created.id())).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> loans.delete(created.id())).isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void paymentRequiresOwnershipOfParentLoanAndPayment() {
        AppUser owner = user("payment.owner@example.com");
        AppUser other = user("payment.other@example.com");

        authenticate(owner);
        LoanView loan = loans.create(loanRequest());
        LoanPaymentRequest request = paymentRequest();
        LoanPaymentView payment = payments.create(loan.id(), request);
        assertThat(payment.loanId()).isEqualTo(loan.id());
        assertThat(payments.all(loan.id(), org.springframework.data.domain.PageRequest.of(0, 20))
                        .getTotalElements())
                .isEqualTo(1);


        authenticate(other);
        assertThatThrownBy(() -> payments.create(loan.id(), request)).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> payments.all(loan.id(), org.springframework.data.domain.PageRequest.of(0, 20)))
                .isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> payments.one(loan.id(), payment.id())).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> payments.update(loan.id(), payment.id(), request))
                .isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> payments.delete(loan.id(), payment.id())).isInstanceOf(ResponseStatusException.class);


        authenticate(owner);
        assertThat(payments.one(loan.id(), payment.id()).id()).isEqualTo(payment.id());
    }

    private AppUser user(String email) {
        return users.save(new AppUser(UUID.randomUUID(), email, null));
    }

    /**
     * Installs a test authentication whose JWT subject identifies the supplied user, allowing the service to resolve tenant ownership exactly as it does in a request.
     */
    private void authenticate(AppUser user) {
        Instant now = Instant.now();
        Jwt jwt = Jwt.withTokenValue("financial-api-test-token")
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
     * Builds a representative international-transfer request with the supplied sent and received currency codes.
     */
    private TransferRequest transferRequest(String sent, String received) {
        return new TransferRequest(
                "Recipient",
                "India",
                new BigDecimal("100.00"),
                sent,
                new BigDecimal("8300.00"),
                received,
                new BigDecimal("83.000000"),
                new BigDecimal("2.00"),
                "bank",
                "family support",
                "pending",
                LocalDate.of(2026, 1, 10),
                null,
                "REF-1",
                null,
                null);
    }

    /**
     * Builds a valid loan request fixture used by the loan and nested-payment ownership scenarios.
     */
    private LoanRequest loanRequest() {
        return new LoanRequest(
                "Personal loan",
                new BigDecimal("1000.00"),
                new BigDecimal("900.00"),
                new BigDecimal("7.50"),
                new BigDecimal("100.00"),
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2027, 1, 1),
                "Bank",
                "personal",
                "active",
                "INR");
    }

    /**
     * Builds a valid loan-payment request fixture for the nested resource scenarios.
     */
    private LoanPaymentRequest paymentRequest() {
        return new LoanPaymentRequest(
                new BigDecimal("100.00"),
                new BigDecimal("90.00"),
                new BigDecimal("10.00"),
                LocalDate.of(2026, 2, 1),
                "bank transfer",
                "monthly payment",
                "INR",
                null,
                null);
    }
}
