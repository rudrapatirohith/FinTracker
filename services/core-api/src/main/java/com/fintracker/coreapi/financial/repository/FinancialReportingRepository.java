package com.fintracker.coreapi.financial.repository;

import com.fintracker.coreapi.financial.dto.response.FinancialReportRow;
import com.fintracker.coreapi.financial.dto.response.FinancialTrendRow;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/**
 * Reads pre-calculated financial summaries from PostgreSQL.
 *
 * <p>This class is the database layer for dashboard and analytics read models.
 * It does not create or update financial records. Instead, it combines rows
 * from the existing financial tables and lets PostgreSQL calculate totals.</p>
 *
 * <p>Important rules:
 * <ul>
 *   <li>Every financial source is filtered by the authenticated user's ID.</li>
 *   <li>Every period-based source uses an inclusive from/to date range.</li>
 *   <li>Amounts are grouped by currency; USD, INR, CAD, etc. are never added together.</li>
 *   <li>Dedicated income/expense records and generic transactions stay separate
 *       metrics so the same real-world event is not silently counted twice.</li>
 * </ul>
 * </p>
 */
@Repository
public class FinancialReportingRepository {

    /*
     * Builds the dashboard/report result in two stages:
     *
     * 1. The inner query selects a common set of columns from each financial
     *    table. UNION ALL stacks those source rows vertically without removing
     *    duplicates. We intentionally keep duplicates because two legitimate
     *    financial entries can have identical values.
     * 2. The outer query groups those rows by metric, category, and currency,
     *    then calculates the total amount and number of records in each group.
     *
     * Each SELECT has the same four output columns: metric, category, currency,
     * and amount. The question marks are JDBC parameters, not string
     * concatenation. Each source receives the same user ID and date range.
     */
    private static final String REPORT_SQL = """
            SELECT metric, category, currency, SUM(amount) AS total_amount, COUNT(*) AS record_count
            FROM (
                -- Dedicated income entries recorded during the requested period.
                SELECT 'INCOME' AS metric,
                       COALESCE(NULLIF(BTRIM(category), ''), 'Uncategorized') AS category,
                       UPPER(COALESCE(NULLIF(BTRIM(currency), ''), 'USD')) AS currency,
                       amount
                FROM income
                WHERE user_id = ? AND date >= ? AND date <= ?

                UNION ALL

                -- Monthly expense entries. Kept separate from generic EXPENSE transactions.
                SELECT 'MONTHLY_EXPENSE' AS metric,
                       COALESCE(NULLIF(BTRIM(category), ''), 'Uncategorized') AS category,
                       UPPER(COALESCE(NULLIF(BTRIM(currency), ''), 'INR')) AS currency,
                       amount
                FROM monthly_expenses
                WHERE user_id = ? AND expense_date >= ? AND expense_date <= ?

                UNION ALL

                -- Actual loan repayments made in the requested period.
                -- This is the payment amount (principal plus interest), not the remaining balance.
                SELECT 'LOAN_PAYMENT' AS metric, 'Loan Payments' AS category,
                       UPPER(COALESCE(NULLIF(BTRIM(currency), ''), 'INR')) AS currency,
                       amount
                FROM loan_payments
                WHERE user_id = ? AND payment_date >= ? AND payment_date <= ?

                UNION ALL

                -- Money sent out in an international transfer.
                -- Use amount_sent/currency_sent so we do not add the converted
                -- amount_received as if it were a second expense.
                SELECT 'TRANSFER' AS metric, 'Transfers' AS category,
                       UPPER(COALESCE(NULLIF(BTRIM(currency_sent), ''), 'USD')) AS currency,
                       amount_sent AS amount
                FROM international_transfers
                WHERE user_id = ? AND transfer_date >= ? AND transfer_date <= ?

                UNION ALL

                -- A scheduled payment is historical spending only after it is paid.
                -- Pending, overdue, or cancelled scheduled items are not counted here.
                SELECT 'SCHEDULED_PAYMENT' AS metric,
                       COALESCE(NULLIF(BTRIM(category), ''), 'Uncategorized') AS category,
                       UPPER(COALESCE(NULLIF(BTRIM(currency), ''), 'USD')) AS currency,
                       amount
                FROM scheduled_payments
                WHERE user_id = ? AND status = 'paid' AND due_date >= ? AND due_date <= ?

                UNION ALL

                -- Generic transactions are labelled by their type (for example,
                -- TRANSACTION_INCOME or TRANSACTION_EXPENSE). They are not merged
                -- into the dedicated income/monthly-expense metrics.
                SELECT 'TRANSACTION_' || UPPER(type) AS metric,
                       COALESCE(NULLIF(BTRIM(category), ''), 'Uncategorized') AS category,
                       UPPER(COALESCE(NULLIF(BTRIM(currency), ''), 'USD')) AS currency,
                       amount
                FROM transactions
                WHERE user_id = ? AND transaction_date >= ? AND transaction_date <= ?
                  AND UPPER(type) IN ('INCOME', 'EXPENSE')
            ) financial_rows
            -- One output row per metric + category + currency combination.
            GROUP BY metric, category, currency
            ORDER BY metric, currency, category
            """;

    /*
     * Produces chart-ready monthly totals.
     *
     * The inner query uses the same financial sources and safety filters as
     * REPORT_SQL, but also exposes each source's date as event_date.
     * DATE_TRUNC('month', event_date) puts dates in the same calendar month;
     * TO_CHAR formats that month as YYYY-MM (for example, 2026-01).
     *
     * The outer GROUP BY creates one row for each month + metric + currency.
     * This means a chart can plot income and expenses separately and cannot
     * accidentally combine different currencies.
     */
    private static final String MONTHLY_TREND_SQL = """
            SELECT TO_CHAR(DATE_TRUNC('month', event_date), 'YYYY-MM') AS period,
                   metric, currency, SUM(amount) AS total_amount, COUNT(*) AS record_count
            FROM (
                -- Income grouped by the month in which it was recorded.
                SELECT date AS event_date, 'INCOME' AS metric,
                       UPPER(COALESCE(NULLIF(BTRIM(currency), ''), 'USD')) AS currency, amount
                FROM income
                WHERE user_id = ? AND date >= ? AND date <= ?

                UNION ALL

                -- Monthly expenses grouped by their expense date.
                SELECT expense_date AS event_date, 'MONTHLY_EXPENSE' AS metric,
                       UPPER(COALESCE(NULLIF(BTRIM(currency), ''), 'INR')) AS currency, amount
                FROM monthly_expenses
                WHERE user_id = ? AND expense_date >= ? AND expense_date <= ?

                UNION ALL

                -- Loan repayments grouped by the date the payment was made.
                SELECT payment_date AS event_date, 'LOAN_PAYMENT' AS metric,
                       UPPER(COALESCE(NULLIF(BTRIM(currency), ''), 'INR')) AS currency, amount
                FROM loan_payments
                WHERE user_id = ? AND payment_date >= ? AND payment_date <= ?

                UNION ALL

                -- Transfers use the sent side only, to avoid counting converted
                -- received money as another event.
                SELECT transfer_date AS event_date, 'TRANSFER' AS metric,
                       UPPER(COALESCE(NULLIF(BTRIM(currency_sent), ''), 'USD')) AS currency, amount_sent
                FROM international_transfers
                WHERE user_id = ? AND transfer_date >= ? AND transfer_date <= ?

                UNION ALL

                -- Only completed-as-paid scheduled payments belong in historical trends.
                SELECT due_date AS event_date, 'SCHEDULED_PAYMENT' AS metric,
                       UPPER(COALESCE(NULLIF(BTRIM(currency), ''), 'USD')) AS currency, amount
                FROM scheduled_payments
                WHERE user_id = ? AND status = 'paid' AND due_date >= ? AND due_date <= ?

                UNION ALL

                -- Generic transactions remain their own metrics.
                SELECT transaction_date AS event_date, 'TRANSACTION_' || UPPER(type) AS metric,
                       UPPER(COALESCE(NULLIF(BTRIM(currency), ''), 'USD')) AS currency, amount
                FROM transactions
                WHERE user_id = ? AND transaction_date >= ? AND transaction_date <= ?
                  AND UPPER(type) IN ('INCOME', 'EXPENSE')
            ) financial_events
            -- DATE_TRUNC groups all dates in a month together; metric/currency
            -- keep unlike financial concepts and currencies in separate rows.
            GROUP BY DATE_TRUNC('month', event_date), metric, currency
            ORDER BY period, metric, currency
            """;

    /*
     * Returns the currently stored outstanding balances for loans that had started
     * on or before the report's 'to' date.
     *
     * Unlike the activity queries above, this query does not sum payments made
     * between from and to. It reads each loan's current_balance and groups those
     * values by loan status and currency. The start-date condition excludes loans
     * that had not yet begun by the requested end date.
     *
     * Important limitation: current_balance is the loan's present stored value.
     * This query does not reconstruct a historical balance as it existed on the
     * requested 'to' date. A true historical balance would require dated balance
     * snapshots or a complete principal/payment ledger and a separate calculation.
     */
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

    /*
     * Converts one SQL result row into the API's summary DTO.
     * Column names here match the aliases in REPORT_SQL and LOAN_BALANCE_SQL.
     */
    private static final RowMapper<FinancialReportRow> REPORT_ROW_MAPPER =
            (rs, row) -> new FinancialReportRow(
                    rs.getString("metric"),
                    rs.getString("category"),
                    rs.getString("currency"),
                    rs.getBigDecimal("total_amount"),
                    rs.getLong("record_count"));

    /*
     * Converts one monthly SQL result row into the API's trend DTO.
     */
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

    /**
     * Runs the period summary query, then appends the loan-balance snapshot.
     *
     * <p>The six groups of (userId, from, to) values correspond to the six
     * SELECT statements in REPORT_SQL. The loan query needs only (userId, to)
     * because it is a point-in-time snapshot rather than period activity.</p>
     */
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

    /**
     * Runs the monthly trend query. Each source contributes (userId, from, to)
     * in the same order as its placeholders appear in MONTHLY_TREND_SQL.
     */
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
