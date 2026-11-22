package com.fintracker.coreapi.financial;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fintracker.coreapi.financial.dto.request.LoanPaymentRequest;
import com.fintracker.coreapi.financial.dto.request.LoanRequest;
import com.fintracker.coreapi.financial.dto.request.TransferRequest;
import com.fintracker.coreapi.financial.dto.response.LoanPaymentView;
import com.fintracker.coreapi.financial.dto.response.LoanView;
import com.fintracker.coreapi.financial.dto.response.TransferView;
import com.fintracker.coreapi.financial.service.LoanApiService;
import com.fintracker.coreapi.financial.service.LoanPaymentApiService;
import com.fintracker.coreapi.financial.service.TransferApiService;
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
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.server.ResponseStatusException;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class FinancialApiOwnershipPostgresIntegrationTests {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("fintracker_financial_api_test")
            .withUsername("fintracker_test")
            .withPassword("test_password");

    /**
     * Registers the Testcontainers database connection and enables Flyway migrations with Hibernate schema validation.
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
    MockMvc mockMvc;

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

    /**
     * Clears authentication and removes persisted users before each test so ownership scenarios start from isolated data.
     */
    @BeforeEach
    void clean() {
        SecurityContextHolder.clearContext();
        jdbc.update("DELETE FROM app_users");
    }
    /**
     * Clears the thread-local security context after each test to prevent one scenario's identity leaking into another.
     */
    @AfterEach
    void clearAuth() {
        SecurityContextHolder.clearContext();
    }

    /**
     * Confirms that transfer CRUD is available to its owner and that another authenticated user cannot read, update, or delete it.
     */
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

    /**
     * Confirms that a loan can be managed by its owner but is hidden from a different authenticated user.
     */
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

    /**
     * Confirms that payment operations require ownership of both the parent loan and the payment record.
     */
    @Test
    void paymentRequiresOwnershipOfParentLoanAndPayment() {
        AppUser owner = user("payment.owner@example.com");
        AppUser other = user("payment.other@example.com");
        authenticate(owner);
        LoanView loan = loans.create(loanRequest());
        LoanPaymentRequest request = paymentRequest();
        LoanPaymentView payment = payments.create(loan.id(), request);
        assertThat(payment.loanId()).isEqualTo(loan.id());
        assertThat(payment.currency()).isEqualTo(loan.currency());
        assertThat(loans.one(loan.id()).currentBalance()).isEqualByComparingTo("810.00");
        assertThat(payments.update(loan.id(), payment.id(), request).currency()).isEqualTo(loan.currency());

        LoanPaymentRequest revisedRequest = new LoanPaymentRequest(
                new BigDecimal("100.00"),
                new BigDecimal("80.00"),
                new BigDecimal("20.00"),
                LocalDate.of(2026, 2, 1),
                "bank transfer",
                "revised principal split",
                null,
                null,
                null);
        payments.update(loan.id(), payment.id(), revisedRequest);
        assertThat(loans.one(loan.id()).currentBalance()).isEqualByComparingTo("820.00");
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
        payments.delete(loan.id(), payment.id());
        assertThat(loans.one(loan.id()).currentBalance()).isEqualByComparingTo("900.00");

        LoanPaymentRequest overpayment = new LoanPaymentRequest(
                new BigDecimal("901.00"),
                new BigDecimal("901.00"),
                BigDecimal.ZERO,
                LocalDate.of(2026, 3, 1),
                "bank transfer",
                null,
                null,
                null,
                null);
        assertThatThrownBy(() -> payments.create(loan.id(), overpayment))
                .isInstanceOf(ResponseStatusException.class);
        assertThat(loans.one(loan.id()).currentBalance()).isEqualByComparingTo("900.00");

        LoanPaymentRequest mismatchedCurrency = new LoanPaymentRequest(
                new BigDecimal("100.00"),
                new BigDecimal("90.00"),
                new BigDecimal("10.00"),
                LocalDate.of(2026, 3, 2),
                "bank transfer",
                null,
                "USD",
                null,
                null);
        assertThatThrownBy(() -> payments.create(loan.id(), mismatchedCurrency))
                .isInstanceOf(ResponseStatusException.class);

        LoanPaymentRequest invalidBreakdown = new LoanPaymentRequest(
                new BigDecimal("100.00"),
                new BigDecimal("90.00"),
                new BigDecimal("9.00"),
                LocalDate.of(2026, 3, 3),
                "bank transfer",
                null,
                null,
                null,
                null);
        assertThatThrownBy(() -> payments.create(loan.id(), invalidBreakdown))
                .isInstanceOf(ResponseStatusException.class);
        assertThat(loans.one(loan.id()).currentBalance()).isEqualByComparingTo("900.00");
    }

    @Test
    void financialEndpointsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/income"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void collectionEndpointRejectsUnapprovedSortProperties() throws Exception {
        AppUser owner = user("pagination.owner@example.com");

        mockMvc.perform(get("/api/v1/income")
                        .param("sort", "userId,asc")
                        .with(jwt().jwt(token -> token.subject(owner.getId().toString()))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void loanEndpointDoesNotExposeAnotherUsersRecord() throws Exception {
        AppUser owner = user("loan.http.owner@example.com");
        AppUser other = user("loan.http.other@example.com");
        authenticate(owner);
        LoanView loan = loans.create(loanRequest());

        mockMvc.perform(get("/api/v1/loans/{id}", loan.id())
                        .with(jwt().jwt(token -> token.subject(other.getId().toString()))))
                .andExpect(status().isNotFound());
    }

    /**
     * Persists a distinct application user used as a tenant identity in the ownership tests.
     */
    private AppUser user(String email) {
        return users.save(new AppUser(UUID.randomUUID(), email, null));
    }
    /**
     * Installs a JWT-backed test principal whose subject is the supplied application's user UUID.
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
     * Builds a valid transfer request while allowing the sent and received currency codes to vary by scenario.
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
     * Builds a representative loan request used by loan and loan-payment ownership scenarios.
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
     * Builds a representative payment request for the nested loan-payment endpoint.
     */
    private LoanPaymentRequest paymentRequest() {
        return new LoanPaymentRequest(
                new BigDecimal("100.00"),
                new BigDecimal("90.00"),
                new BigDecimal("10.00"),
                LocalDate.of(2026, 2, 1),
                "bank transfer",
                "monthly payment",
                null,
                null,
                null);
    }
}
