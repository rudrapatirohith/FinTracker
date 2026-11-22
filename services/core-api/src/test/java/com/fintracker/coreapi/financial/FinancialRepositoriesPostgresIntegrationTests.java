package com.fintracker.coreapi.financial;

import static org.assertj.core.api.Assertions.assertThat;

import com.fintracker.coreapi.financial.entity.FinancialTransaction;
import com.fintracker.coreapi.financial.entity.InternationalTransfer;
import com.fintracker.coreapi.financial.entity.Loan;
import com.fintracker.coreapi.financial.entity.LoanPayment;
import com.fintracker.coreapi.financial.entity.MonthlyExpense;
import com.fintracker.coreapi.financial.entity.ScheduledPayment;
import com.fintracker.coreapi.financial.repository.FinancialTransactionRepository;
import com.fintracker.coreapi.financial.repository.InternationalTransferRepository;
import com.fintracker.coreapi.financial.repository.LoanPaymentRepository;
import com.fintracker.coreapi.financial.repository.LoanRepository;
import com.fintracker.coreapi.financial.repository.MonthlyExpenseRepository;
import com.fintracker.coreapi.financial.repository.ScheduledPaymentRepository;
import com.fintracker.coreapi.identity.entity.AppUser;
import com.fintracker.coreapi.identity.repository.AppUserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class FinancialRepositoriesPostgresIntegrationTests {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("fintracker_financial_repositories_test")
            .withUsername("fintracker_test")
            .withPassword("test_password");

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
    private AppUserRepository appUserRepository;

    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private LoanPaymentRepository loanPaymentRepository;

    @Autowired
    private InternationalTransferRepository internationalTransferRepository;

    @Autowired
    private MonthlyExpenseRepository monthlyExpenseRepository;

    @Autowired
    private ScheduledPaymentRepository scheduledPaymentRepository;

    @Autowired
    private FinancialTransactionRepository financialTransactionRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.update("DELETE FROM app_users");
    }

    @Test
    void financialRepositoriesShouldRestrictQueriesToTheOwningUser() {

        AppUser userA = appUserRepository.save(new AppUser(UUID.randomUUID(), "financial.user.a@example.com", null));

        AppUser userB = appUserRepository.save(new AppUser(UUID.randomUUID(), "financial.user.b@example.com", null));

        UUID userAId = userA.getId();
        UUID userBId = userB.getId();

        // Loans and their payments.
        Loan loanA = loanRepository.saveAndFlush(new Loan(
                userAId,
                "User A loan",
                new BigDecimal("10000.00"),
                new BigDecimal("8000.00"),
                LocalDate.of(2026, 1, 1)));

        Loan loanB = loanRepository.saveAndFlush(new Loan(
                userBId,
                "User B loan",
                new BigDecimal("20000.00"),
                new BigDecimal("15000.00"),
                LocalDate.of(2026, 1, 1)));

        LoanPayment paymentA = loanPaymentRepository.save(
                new LoanPayment(loanA.getId(), userAId, new BigDecimal("500.00"), LocalDate.of(2026, 2, 1)));

        LoanPayment paymentB = loanPaymentRepository.save(
                new LoanPayment(loanB.getId(), userBId, new BigDecimal("750.00"), LocalDate.of(2026, 2, 1)));

        // International transfers.
        InternationalTransfer transferA = internationalTransferRepository.save(new InternationalTransfer(
                userAId,
                "Recipient A",
                "India",
                new BigDecimal("100.00"),
                new BigDecimal("8300.00"),
                new BigDecimal("83.00"),
                LocalDate.of(2026, 2, 2)));

        InternationalTransfer transferB = internationalTransferRepository.save(new InternationalTransfer(
                userBId,
                "Recipient B",
                "India",
                new BigDecimal("200.00"),
                new BigDecimal("16600.00"),
                new BigDecimal("83.00"),
                LocalDate.of(2026, 2, 2)));

        // Monthly expenses.
        MonthlyExpense expenseA = monthlyExpenseRepository.save(
                new MonthlyExpense(userAId, "Rent A", new BigDecimal("1200.00"), LocalDate.of(2026, 2, 3)));

        MonthlyExpense expenseB = monthlyExpenseRepository.save(
                new MonthlyExpense(userBId, "Rent B", new BigDecimal("1500.00"), LocalDate.of(2026, 2, 3)));

        // Scheduled payments.
        ScheduledPayment scheduledA = scheduledPaymentRepository.save(
                new ScheduledPayment(userAId, "Electricity A", new BigDecimal("100.00"), LocalDate.of(2026, 2, 10)));

        ScheduledPayment scheduledB = scheduledPaymentRepository.save(
                new ScheduledPayment(userBId, "Electricity B", new BigDecimal("125.00"), LocalDate.of(2026, 2, 10)));

        // Financial transactions.
        FinancialTransaction transactionA = financialTransactionRepository.save(new FinancialTransaction(
                userAId, "EXPENSE", "Food", new BigDecimal("45.00"), LocalDate.of(2026, 2, 4)));

        FinancialTransaction transactionB = financialTransactionRepository.save(new FinancialTransaction(
                userBId, "EXPENSE", "Transport", new BigDecimal("30.00"), LocalDate.of(2026, 2, 4)));

        PageRequest page = PageRequest.of(0, 20);

        // Verify each collection is scoped to its owner.
        assertThat(loanRepository.findAllByUserId(userAId, page).getContent())
                .hasSize(1)
                .allMatch(item -> item.getUserId().equals(userAId));

        assertThat(loanRepository.findAllByUserId(userBId, page).getContent())
                .hasSize(1)
                .allMatch(item -> item.getUserId().equals(userBId));

        assertThat(loanPaymentRepository.findAllByUserId(userAId, page).getContent())
                .hasSize(1)
                .allMatch(item -> item.getUserId().equals(userAId));

        assertThat(loanPaymentRepository.findAllByUserId(userBId, page).getContent())
                .hasSize(1)
                .allMatch(item -> item.getUserId().equals(userBId));

        assertThat(internationalTransferRepository
                        .findAllByUserId(userAId, page)
                        .getContent())
                .hasSize(1)
                .allMatch(item -> item.getUserId().equals(userAId));

        assertThat(internationalTransferRepository
                        .findAllByUserId(userBId, page)
                        .getContent())
                .hasSize(1)
                .allMatch(item -> item.getUserId().equals(userBId));

        assertThat(monthlyExpenseRepository.findAllByUserId(userAId, page).getContent())
                .hasSize(1)
                .allMatch(item -> item.getUserId().equals(userAId));

        assertThat(monthlyExpenseRepository.findAllByUserId(userBId, page).getContent())
                .hasSize(1)
                .allMatch(item -> item.getUserId().equals(userBId));

        assertThat(scheduledPaymentRepository.findAllByUserId(userAId, page).getContent())
                .hasSize(1)
                .allMatch(item -> item.getUserId().equals(userAId));

        assertThat(scheduledPaymentRepository.findAllByUserId(userBId, page).getContent())
                .hasSize(1)
                .allMatch(item -> item.getUserId().equals(userBId));

        assertThat(financialTransactionRepository.findAllByUserId(userAId, page).getContent())
                .hasSize(1)
                .allMatch(item -> item.getUserId().equals(userAId));

        assertThat(financialTransactionRepository.findAllByUserId(userBId, page).getContent())
                .hasSize(1)
                .allMatch(item -> item.getUserId().equals(userBId));

        // Verify ID lookups cannot cross user boundaries.
        assertThat(loanRepository.findByIdAndUserId(loanA.getId(), userAId)).isPresent();
        assertThat(loanRepository.findByIdAndUserId(loanA.getId(), userBId)).isEmpty();

        assertThat(loanPaymentRepository.findByIdAndUserId(paymentA.getId(), userAId))
                .isPresent();
        assertThat(loanPaymentRepository.findByIdAndUserId(paymentA.getId(), userBId))
                .isEmpty();

        assertThat(internationalTransferRepository.findByIdAndUserId(transferA.getId(), userAId))
                .isPresent();
        assertThat(internationalTransferRepository.findByIdAndUserId(transferA.getId(), userBId))
                .isEmpty();

        assertThat(monthlyExpenseRepository.findByIdAndUserId(expenseA.getId(), userAId))
                .isPresent();
        assertThat(monthlyExpenseRepository.findByIdAndUserId(expenseA.getId(), userBId))
                .isEmpty();

        assertThat(scheduledPaymentRepository.findByIdAndUserId(scheduledA.getId(), userAId))
                .isPresent();
        assertThat(scheduledPaymentRepository.findByIdAndUserId(scheduledA.getId(), userBId))
                .isEmpty();

        assertThat(financialTransactionRepository.findByIdAndUserId(transactionA.getId(), userAId))
                .isPresent();
        assertThat(financialTransactionRepository.findByIdAndUserId(transactionA.getId(), userBId))
                .isEmpty();

        // Verify the additional loan + owner scope on payment queries.
        assertThat(loanPaymentRepository
                        .findAllByLoanIdAndUserId(loanA.getId(), userAId, page)
                        .getContent())
                .hasSize(1);

        assertThat(loanPaymentRepository
                        .findAllByLoanIdAndUserId(loanA.getId(), userBId, page)
                        .getContent())
                .isEmpty();

        assertThat(loanPaymentRepository
                        .findAllByLoanIdAndUserId(loanB.getId(), userAId, page)
                        .getContent())
                .isEmpty();

        // Keep the second user's records referenced explicitly in the test.
        assertThat(paymentB.getUserId()).isEqualTo(userBId);
        assertThat(transferB.getUserId()).isEqualTo(userBId);
        assertThat(expenseB.getUserId()).isEqualTo(userBId);
        assertThat(scheduledB.getUserId()).isEqualTo(userBId);
        assertThat(transactionB.getUserId()).isEqualTo(userBId);
    }
}
