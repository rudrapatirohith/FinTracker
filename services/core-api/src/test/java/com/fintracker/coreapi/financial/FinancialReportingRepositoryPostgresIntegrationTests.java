package com.fintracker.coreapi.financial;

import static org.assertj.core.api.Assertions.assertThat;

import com.fintracker.coreapi.financial.dto.response.FinancialReportRow;
import com.fintracker.coreapi.financial.dto.response.FinancialTrendRow;
import com.fintracker.coreapi.financial.repository.FinancialReportingRepository;
import com.fintracker.coreapi.identity.entity.AppUser;
import com.fintracker.coreapi.identity.repository.AppUserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Runs the real reporting SQL against a disposable PostgreSQL instance.
 *
 * <p>These are integration tests rather than mocked repository tests: Flyway
 * builds the schema, test rows are inserted into the actual tables, and the
 * repository queries PostgreSQL. This catches SQL/schema/parameter mistakes
 * that a mocked unit test would not detect.</p>
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class FinancialReportingRepositoryPostgresIntegrationTests {

    // A real PostgreSQL 16 container keeps the test behavior close to production PostgreSQL.
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("fintracker_reporting_test")
            .withUsername("fintracker_test")
            .withPassword("test_password");

    // Point the Spring test application at the temporary container database.
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
    private FinancialReportingRepository reportingRepository;

    @Autowired
    private AppUserRepository users;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void cleanDatabase() {
        // Deleting users also clears their dependent financial rows via the schema's FK cascade.
        jdbc.update("DELETE FROM app_users");
    }

    @Test
    void summaryAndMonthlyTrendsCoverFinancialSourcesWithTenantCurrencyAndDateIsolation() {
        // Two owners prove that one user's financial data never leaks into another user's report.
        AppUser owner = users.save(new AppUser(UUID.randomUUID(), "report.owner@example.com", null));
        AppUser other = users.save(new AppUser(UUID.randomUUID(), "report.other@example.com", null));
        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to = LocalDate.of(2026, 1, 31);

        // Include first-day and last-day rows to prove both date boundaries are inclusive.
        insertIncome(owner.getId(), "Salary", "1000.00", "USD", LocalDate.of(2026, 1, 1));
        insertIncome(owner.getId(), "Consulting", "500.00", "USD", LocalDate.of(2026, 1, 31));
        insertIncome(owner.getId(), "Contract", "700.00", "INR", LocalDate.of(2026, 1, 15));
        // This row is outside the requested period and must not be counted.
        insertIncome(owner.getId(), "Outside range", "999.00", "USD", LocalDate.of(2026, 2, 1));
        // These rows belong to another user and must never appear in the report.
        insertIncome(other.getId(), "Other tenant", "8000.00", "USD", LocalDate.of(2026, 1, 10));

        insertExpense(owner.getId(), "Groceries", "250.00", "USD", "Food", LocalDate.of(2026, 1, 20));
        insertExpense(other.getId(), "Other tenant expense", "900.00", "USD", "Food", LocalDate.of(2026, 1, 20));

        UUID loanId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO loans
                    (id, user_id, loan_name, principal_amount, current_balance, start_date, status, currency)
                VALUES (?, ?, 'Auto loan', 5000.00, 4500.00, DATE '2025-12-01', 'active', 'INR')
                """, loanId, owner.getId());
        // The payment activity is in January; current_balance stores the currently outstanding amount.
        jdbc.update("""
                INSERT INTO loan_payments
                    (loan_id, user_id, amount, principal_amount, interest_amount, payment_date, currency)
                VALUES (?, ?, 100.00, 90.00, 10.00, DATE '2026-01-22', 'INR')
                """, loanId, owner.getId());

        // Store both sent and received amounts. Reporting should count only the sent side.
        jdbc.update("""
                INSERT INTO international_transfers
                    (user_id, recipient_name, recipient_country, amount_sent, currency_sent,
                     amount_received, currency_received, exchange_rate, transfer_date, status)
                VALUES (?, 'Recipient', 'India', 100.00, 'USD', 8300.00, 'INR', 83.00,
                        DATE '2026-01-23', 'completed')
                """, owner.getId());

        // A paid scheduled payment should count; other statuses are intentionally excluded by the SQL.
        jdbc.update("""
                INSERT INTO scheduled_payments
                    (user_id, payment_name, amount, currency, category, due_date, status)
                VALUES (?, 'Electric bill', 75.00, 'USD', 'Utilities', DATE '2026-01-24', 'paid')
                """, owner.getId());

        jdbc.update("""
                INSERT INTO transactions
                    (user_id, type, category, amount, currency, transaction_date)
                VALUES (?, 'expense', 'Dining', 30.00, 'CAD', DATE '2026-01-25')
                """, owner.getId());

        // Key the result by all grouping dimensions so each expected aggregate is unambiguous.
        List<FinancialReportRow> rows = reportingRepository.summarize(owner.getId(), from, to);
        Map<String, FinancialReportRow> byMetricAndCurrency = rows.stream()
                .collect(Collectors.toMap(
                        row -> row.metric() + ":" + row.currency() + ":" + row.category(),
                        Function.identity()));

        // Verify source totals, separate currencies, and record counts.
        assertThat(byMetricAndCurrency.get("INCOME:USD:Earnings").totalAmount())
                .isEqualByComparingTo("1500.00");
        assertThat(byMetricAndCurrency.get("INCOME:USD:Earnings").recordCount()).isEqualTo(2);
        assertThat(byMetricAndCurrency.get("INCOME:INR:Earnings").totalAmount())
                .isEqualByComparingTo("700.00");
        assertThat(byMetricAndCurrency.get("MONTHLY_EXPENSE:USD:Food").totalAmount())
                .isEqualByComparingTo("250.00");
        assertThat(byMetricAndCurrency.get("LOAN_PAYMENT:INR:Loan Payments").totalAmount())
                .isEqualByComparingTo("100.00");
        assertThat(byMetricAndCurrency.get("TRANSFER:USD:Transfers").totalAmount())
                .isEqualByComparingTo("100.00");
        assertThat(byMetricAndCurrency.get("SCHEDULED_PAYMENT:USD:Utilities").totalAmount())
                .isEqualByComparingTo("75.00");
        assertThat(byMetricAndCurrency.get("TRANSACTION_EXPENSE:CAD:Dining").totalAmount())
                .isEqualByComparingTo("30.00");
        assertThat(byMetricAndCurrency.get("LOAN_OUTSTANDING:INR:active").totalAmount())
                .isEqualByComparingTo("4500.00");
        // Other owner's USD 8,000 and USD 900 records must not leak into this owner's result.
        assertThat(rows).noneMatch(row -> row.totalAmount().compareTo(new BigDecimal("8000.00")) == 0
                || row.totalAmount().compareTo(new BigDecimal("900.00")) == 0);

        // All activity was in January, so each trend row should carry the 2026-01 period.
        List<FinancialTrendRow> trends = reportingRepository.monthlyTrends(owner.getId(), from, to);
        assertThat(trends).hasSize(7);
        assertThat(trends).allSatisfy(row -> assertThat(row.period()).isEqualTo("2026-01"));
        assertThat(trends).noneMatch(row -> row.totalAmount().compareTo(new BigDecimal("8000.00")) == 0);
    }

    // Small helpers keep test setup readable while inserting representative source records.
    private void insertIncome(UUID userId, String source, String amount, String currency, LocalDate date) {
        jdbc.update(
                "INSERT INTO income (user_id, source, amount, currency, category, date) VALUES (?, ?, ?, ?, ?, ?)",
                userId, source, new BigDecimal(amount), currency, "Earnings", date);
    }

    private void insertExpense(UUID userId, String name, String amount, String currency, String category, LocalDate date) {
        jdbc.update(
                "INSERT INTO monthly_expenses (user_id, expense_name, amount, currency, category, expense_date) "
                        + "VALUES (?, ?, ?, ?, ?, ?)",
                userId, name, new BigDecimal(amount), currency, category, date);
    }
}
