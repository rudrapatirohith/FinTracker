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

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class FinancialReportingRepositoryPostgresIntegrationTests {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("fintracker_reporting_test")
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
    private FinancialReportingRepository reportingRepository;

    @Autowired
    private AppUserRepository users;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void cleanDatabase() {
        jdbc.update("DELETE FROM app_users");
    }

    @Test
    void summaryAndMonthlyTrendsAreOwnerScopedCurrencySafeAndDateInclusive() {
        AppUser owner = users.save(new AppUser(UUID.randomUUID(), "report.owner@example.com", null));
        AppUser other = users.save(new AppUser(UUID.randomUUID(), "report.other@example.com", null));
        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to = LocalDate.of(2026, 1, 31);

        insertIncome(owner.getId(), "Salary", "1000.00", "USD", LocalDate.of(2026, 1, 1));
        insertIncome(owner.getId(), "Consulting", "500.00", "USD", LocalDate.of(2026, 1, 31));
        insertIncome(owner.getId(), "Contract", "700.00", "INR", LocalDate.of(2026, 1, 15));
        insertIncome(owner.getId(), "Outside range", "999.00", "USD", LocalDate.of(2026, 2, 1));
        insertIncome(other.getId(), "Other tenant", "8000.00", "USD", LocalDate.of(2026, 1, 10));
        jdbc.update("""
                INSERT INTO monthly_expenses
                    (user_id, expense_name, amount, currency, category, expense_date)
                VALUES (?, 'Groceries', 250.00, 'USD', 'Food', DATE '2026-01-20')
                """, owner.getId());

        List<FinancialReportRow> rows = reportingRepository.summarize(owner.getId(), from, to);
        Map<String, FinancialReportRow> incomeByCurrency = rows.stream()
                .filter(row -> row.metric().equals("INCOME"))
                .collect(Collectors.toMap(FinancialReportRow::currency, Function.identity()));

        assertThat(incomeByCurrency).containsOnlyKeys("USD", "INR");
        assertThat(incomeByCurrency.get("USD").totalAmount()).isEqualByComparingTo("1500.00");
        assertThat(incomeByCurrency.get("USD").recordCount()).isEqualTo(2);
        assertThat(incomeByCurrency.get("INR").totalAmount()).isEqualByComparingTo("700.00");
        assertThat(rows).anySatisfy(row -> {
            assertThat(row.metric()).isEqualTo("MONTHLY_EXPENSE");
            assertThat(row.currency()).isEqualTo("USD");
            assertThat(row.totalAmount()).isEqualByComparingTo("250.00");
        });

        List<FinancialTrendRow> trends = reportingRepository.monthlyTrends(owner.getId(), from, to);
        assertThat(trends).hasSize(3);
        assertThat(trends).allSatisfy(row -> assertThat(row.period()).isEqualTo("2026-01"));
        assertThat(trends).noneMatch(row -> row.totalAmount().compareTo(new BigDecimal("8000.00")) == 0);
    }

    private void insertIncome(UUID userId, String source, String amount, String currency, LocalDate date) {
        jdbc.update(
                "INSERT INTO income (user_id, source, amount, currency, category, date) VALUES (?, ?, ?, ?, ?, ?)",
                userId, source, new BigDecimal(amount), currency, "Earnings", date);
    }
}
