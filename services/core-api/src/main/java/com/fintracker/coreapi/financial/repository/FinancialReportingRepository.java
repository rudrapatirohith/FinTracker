package com.fintracker.coreapi.financial.repository;

import com.fintracker.coreapi.financial.dto.response.FinancialReportRow;
import com.fintracker.coreapi.financial.dto.response.FinancialTrendRow;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/**
 * PostgreSQL read model for financial summaries and analytics.
 *
 * <p>Every source is owner- and date-scoped in SQL. Values are grouped by
 * currency rather than converted with an assumed exchange rate. Financial
 * transactions remain a distinct metric from the dedicated income/expense
 * workflows to avoid silently double-counting legacy records.</p>
 */
@Repository
public class FinancialReportingRepository {

    private static final String REPORT_SQL = """
            SELECT metric, category, currency, SUM(amount) AS total_amount, COUNT(*) AS record_count
            FROM (
                SELECT 'INCOME' AS metric,
                       COALESCE(NULLIF(BTRIM(category), ''), 'Uncategorized') AS category,
                       UPPER(COALESCE(NULLIF(BTRIM(currency), ''), 'USD')) AS currency,
                       amount
                FROM income
                WHERE user_id = ? AND date >= ? AND date <= ?

                UNION ALL

                SELECT 'MONTHLY_EXPENSE' AS metric,
                       COALESCE(NULLIF(BTRIM(category), ''), 'Uncategorized') AS category,
                       UPPER(COALESCE(NULLIF(BTRIM(currency), ''), 'INR')) AS currency,
                       amount
                FROM monthly_expenses
                WHERE user_id = ? AND expense_date >= ? AND expense_date <= ?

                UNION ALL

                SELECT 'LOAN_PAYMENT' AS metric, 'Loan Payments' AS category,
                       UPPER(COALESCE(NULLIF(BTRIM(currency), ''), 'INR')) AS currency,
                       amount
                FROM loan_payments
                WHERE user_id = ? AND payment_date >= ? AND payment_date <= ?

                UNION ALL

                SELECT 'TRANSFER' AS metric, 'Transfers' AS category,
                       UPPER(COALESCE(NULLIF(BTRIM(currency_sent), ''), 'USD')) AS currency,
                       amount_sent AS amount
                FROM international_transfers
                WHERE user_id = ? AND transfer_date >= ? AND transfer_date <= ?

                UNION ALL

                SELECT 'SCHEDULED_PAYMENT' AS metric,
                       COALESCE(NULLIF(BTRIM(category), ''), 'Uncategorized') AS category,
                       UPPER(COALESCE(NULLIF(BTRIM(currency), ''), 'USD')) AS currency,
                       amount
                FROM scheduled_payments
                WHERE user_id = ? AND status = 'paid' AND due_date >= ? AND due_date <= ?

                UNION ALL

                SELECT 'TRANSACTION_' || UPPER(type) AS metric,
                       COALESCE(NULLIF(BTRIM(category), ''), 'Uncategorized') AS category,
                       UPPER(COALESCE(NULLIF(BTRIM(currency), ''), 'USD')) AS currency,
                       amount
                FROM transactions
                WHERE user_id = ? AND transaction_date >= ? AND transaction_date <= ?
                  AND UPPER(type) IN ('INCOME', 'EXPENSE')
            ) financial_rows
            GROUP BY metric, category, currency
            ORDER BY metric, currency, category
            """;

    private static final String MONTHLY_TREND_SQL = """
            SELECT TO_CHAR(DATE_TRUNC('month', event_date), 'YYYY-MM') AS period,
                   metric, currency, SUM(amount) AS total_amount, COUNT(*) AS record_count
            FROM (
                SELECT date AS event_date, 'INCOME' AS metric,
                       UPPER(COALESCE(NULLIF(BTRIM(currency), ''), 'USD')) AS currency, amount
                FROM income
                WHERE user_id = ? AND date >= ? AND date <= ?

                UNION ALL

                SELECT expense_date AS event_date, 'MONTHLY_EXPENSE' AS metric,
                       UPPER(COALESCE(NULLIF(BTRIM(currency), ''), 'INR')) AS currency, amount
                FROM monthly_expenses
                WHERE user_id = ? AND expense_date >= ? AND expense_date <= ?

                UNION ALL

                SELECT payment_date AS event_date, 'LOAN_PAYMENT' AS metric,
                       UPPER(COALESCE(NULLIF(BTRIM(currency), ''), 'INR')) AS currency, amount
                FROM loan_payments
                WHERE user_id = ? AND payment_date >= ? AND payment_date <= ?

                UNION ALL

                SELECT transfer_date AS event_date, 'TRANSFER' AS metric,
                       UPPER(COALESCE(NULLIF(BTRIM(currency_sent), ''), 'USD')) AS currency, amount_sent
                FROM international_transfers
                WHERE user_id = ? AND transfer_date >= ? AND transfer_date <= ?

                UNION ALL

                SELECT due_date AS event_date, 'SCHEDULED_PAYMENT' AS metric,
                       UPPER(COALESCE(NULLIF(BTRIM(currency), ''), 'USD')) AS currency, amount
                FROM scheduled_payments
                WHERE user_id = ? AND status = 'paid' AND due_date >= ? AND due_date <= ?

                UNION ALL

                SELECT transaction_date AS event_date, 'TRANSACTION_' || UPPER(type) AS metric,
                       UPPER(COALESCE(NULLIF(BTRIM(currency), ''), 'USD')) AS currency, amount
                FROM transactions
                WHERE user_id = ? AND transaction_date >= ? AND transaction_date <= ?
                  AND UPPER(type) IN ('INCOME', 'EXPENSE')
            ) financial_events
            GROUP BY DATE_TRUNC('month', event_date), metric, currency
            ORDER BY period, metric, currency
            """;

    private static final String LOAN_BALANCE_SQL = """
            SELECT 'LOAN_OUTSTANDING' AS metric,
                   COALESCE(NULLIF(BTRIM(status), ''), 'unknown') AS category,
                   UPPER(COALESCE(NULLIF(BTRIM(currency), ''), 'INR')) AS currency,
                   SUM(current_balance) AS total_amount,
                   COUNT(*) AS record_count
            FROM loans
            WHERE user_id = ? AND start_date <= ?
            GROUP BY status, currency
            ORDER BY currency, status
            """;

    private static final RowMapper<FinancialReportRow> REPORT_ROW_MAPPER =
            (rs, row) -> new FinancialReportRow(
                    rs.getString("metric"),
                    rs.getString("category"),
                    rs.getString("currency"),
                    rs.getBigDecimal("total_amount"),
                    rs.getLong("record_count"));

    private static final RowMapper<FinancialTrendRow> TREND_ROW_MAPPER =
            (rs, row) -> new FinancialTrendRow(
                    rs.getString("period"),
                    rs.getString("metric"),
                    rs.getString("currency"),
                    rs.getBigDecimal("total_amount"),
                    rs.getLong("record_count"));

    private final JdbcTemplate jdbcTemplate;

    public FinancialReportingRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<FinancialReportRow> summarize(UUID userId, LocalDate from, LocalDate to) {
        List<FinancialReportRow> rows = jdbcTemplate.query(
                REPORT_SQL,
                REPORT_ROW_MAPPER,
                userId, from, to,
                userId, from, to,
                userId, from, to,
                userId, from, to,
                userId, from, to,
                userId, from, to);
        rows.addAll(jdbcTemplate.query(LOAN_BALANCE_SQL, REPORT_ROW_MAPPER, userId, to));
        return rows;
    }

    public List<FinancialTrendRow> monthlyTrends(UUID userId, LocalDate from, LocalDate to) {
        return jdbcTemplate.query(
                MONTHLY_TREND_SQL,
                TREND_ROW_MAPPER,
                userId, from, to,
                userId, from, to,
                userId, from, to,
                userId, from, to,
                userId, from, to,
                userId, from, to);
    }
}
