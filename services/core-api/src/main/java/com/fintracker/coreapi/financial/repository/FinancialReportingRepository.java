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
 * Gathers financial totals and monthly chart numbers from the database.
 *
 * <p>Think of this file as an accountant that goes through 6 different folders
 * (Income, Expenses, Loan Payments, Transfers, Scheduled Bills, and General Transactions),
 * sorts all your receipts into neat piles, and adds up the totals for your dashboard.</p>
 *
 * <p><b>4 Golden Rules this file always follows:</b>
 * <ul>
 *   <li><b>Privacy first:</b> Every search checks your personal User ID so you never see anyone else's money.</li>
 *   <li><b>Exact date window:</b> Activity records use your chosen Start Date and End Date. Loan balances are a separate current snapshot, not historical activity.</li>
 *   <li><b>Never mixes currencies:</b> Dollars (USD), Rupees (INR), and Canadian Dollars (CAD) are kept in separate totals—never added together.</li>
 *   <li><b>Canonical event counting:</b> Dedicated domain records are the source of truth. A generic history mirror must reference its same-owner source row; reporting then counts the domain record once. Standalone generic transactions remain included.</li>
 * </ul>
 * </p>
 */
@Repository
public class FinancialReportingRepository {

    /*
     * Shared source data for both period summaries and monthly trends.
     *
     * Think of this as collecting all supported financial events into one
     * standard list before deciding how to display them. Each SELECT returns
     * the same five columns: event date, metric, category, currency, and amount.
     *
     * Keeping the source definitions in one place prevents the dashboard query
     * and monthly chart query from quietly drifting apart when a source or
     * business rule changes. The six userId placeholders are bound by Java;
     * the date range is applied by the outer query in each report below.
     */
    private static final String FINANCIAL_EVENTS_CTE = """
            WITH financial_events AS (
                -- Source 1: dedicated income records.
                SELECT date AS event_date, 'INCOME' AS metric,
                       COALESCE(NULLIF(BTRIM(category), ''), 'Uncategorized') AS category,
                       UPPER(COALESCE(NULLIF(BTRIM(currency), ''), 'USD')) AS currency,
                       amount
                FROM income
                WHERE user_id = ?

                UNION ALL

                -- Source 2: monthly expense records; kept separate from generic transactions.
                SELECT expense_date AS event_date, 'MONTHLY_EXPENSE' AS metric,
                       COALESCE(NULLIF(BTRIM(category), ''), 'Uncategorized') AS category,
                       UPPER(COALESCE(NULLIF(BTRIM(currency), ''), 'INR')) AS currency,
                       amount
                FROM monthly_expenses
                WHERE user_id = ?

                UNION ALL

                -- Source 3: loan repayments made on payment_date (not outstanding loan balances).
                SELECT payment_date AS event_date, 'LOAN_PAYMENT' AS metric,
                       'Loan Payments' AS category,
                       UPPER(COALESCE(NULLIF(BTRIM(currency), ''), 'INR')) AS currency,
                       amount
                FROM loan_payments
                WHERE user_id = ?

                UNION ALL

                -- Source 4: international transfers. Report sent money only, not the converted
                -- received amount or the transfer fee. All statuses are included to match the
                -- existing frontend's all-transfer total, including pending transfers.
                SELECT transfer_date AS event_date, 'TRANSFER' AS metric,
                       'Transfers' AS category,
                       UPPER(COALESCE(NULLIF(BTRIM(currency_sent), ''), 'USD')) AS currency,
                       amount_sent AS amount
                FROM international_transfers
                WHERE user_id = ?

                UNION ALL

                -- Source 5: scheduled payments marked paid. The current schema has no paid_at
                -- date, so event_date is due_date; late payments are attributed to the due period.
                SELECT due_date AS event_date, 'SCHEDULED_PAYMENT' AS metric,
                       COALESCE(NULLIF(BTRIM(category), ''), 'Uncategorized') AS category,
                       UPPER(COALESCE(NULLIF(BTRIM(currency), ''), 'USD')) AS currency,
                       amount
                FROM scheduled_payments
                WHERE user_id = ? AND status = 'paid'

                UNION ALL

                -- Source 6: standalone generic income/expense transactions. A row linked to
                -- a same-owner domain record through reference_id is a history mirror, not a
                -- second financial event, and is excluded by the NOT EXISTS checks below.
                SELECT t.transaction_date AS event_date, 'TRANSACTION_' || UPPER(t.type) AS metric,
                       COALESCE(NULLIF(BTRIM(t.category), ''), 'Uncategorized') AS category,
                       UPPER(COALESCE(NULLIF(BTRIM(t.currency), ''), 'USD')) AS currency,
                       t.amount
                FROM transactions t
                WHERE t.user_id = ? AND UPPER(t.type) IN ('INCOME', 'EXPENSE')
                  -- A transaction may also appear in a domain table for unified history.
                  -- When reference_id points to that same user's canonical source row, count
                  -- the domain row only; otherwise the generic transaction is standalone.
                  AND (t.reference_id IS NULL OR (
                      NOT EXISTS (SELECT 1 FROM income i
                                  WHERE i.id = t.reference_id AND i.user_id = t.user_id)
                      AND NOT EXISTS (SELECT 1 FROM monthly_expenses e
                                      WHERE e.id = t.reference_id AND e.user_id = t.user_id)
                      AND NOT EXISTS (SELECT 1 FROM loan_payments lp
                                      WHERE lp.id = t.reference_id AND lp.user_id = t.user_id)
                      AND NOT EXISTS (SELECT 1 FROM international_transfers tr
                                      WHERE tr.id = t.reference_id AND tr.user_id = t.user_id)
                      AND NOT EXISTS (SELECT 1 FROM scheduled_payments sp
                                      WHERE sp.id = t.reference_id AND sp.user_id = t.user_id)
                  ))
            )
            """;

    /*
     * QUERY 1: TOTALS FOR A SELECTED DATE RANGE
     *
     * The shared CTE above supplies the six source tables. This query decides
     * how to summarize them: one row for each metric + category + currency.
     * SUM adds the money in each group; COUNT tells us how many records were used.
     * Different currencies and different financial sources stay in separate rows.
     */
    private static final String REPORT_SQL = FINANCIAL_EVENTS_CTE + """
            SELECT metric, category, currency,
                   SUM(amount) AS total_amount,
                   COUNT(*) AS record_count,
                   NULL::date AS snapshot_date
            FROM financial_events
            WHERE event_date >= ? AND event_date <= ?
            GROUP BY metric, category, currency
            ORDER BY metric, currency, category
            """;

    /*
     * QUERY 2: MONTH-BY-MONTH CHART DATA
     *
     * It uses the exact same financial events and filters as Query 1, but groups
     * them by calendar month instead of category. DATE_TRUNC puts all dates in
     * the same month together; TO_CHAR returns a chart-friendly value such as
     * "2026-01". The metric and currency remain separate.
     */
    private static final String MONTHLY_TREND_SQL = FINANCIAL_EVENTS_CTE + """
            SELECT TO_CHAR(DATE_TRUNC('month', event_date), 'YYYY-MM') AS period,
                   metric,
                   currency,
                   SUM(amount) AS total_amount,
                   COUNT(*) AS record_count
            FROM financial_events
            WHERE event_date >= ? AND event_date <= ?
            GROUP BY DATE_TRUNC('month', event_date), metric, currency
            ORDER BY period, metric, currency
            """;

    /*
     * =========================================================================
     * QUERY 3: CURRENT LOAN DEBT SNAPSHOT (LOAN_BALANCE_SQL)
     * =========================================================================
     * What it does:
     * Checks how much total money you currently still owe on your loans.
     *
     * Important detail to keep in mind:
     * Unlike Queries 1 & 2 (which look at money moving in and out during a date
     * range), this checks your loans' CURRENT remaining balance today—as long as
     * the loan started on or before your chosen End Date. Even if you look at a
     * report for last year, this still shows the database's current remaining balance,
     * not what the balance was back then. The response includes snapshotDate for clarity.
     */
    private static final String LOAN_BALANCE_SQL = """
            SELECT 'LOAN_OUTSTANDING' AS metric,
                   COALESCE(NULLIF(BTRIM(status), ''), 'unknown') AS category,
                   UPPER(COALESCE(NULLIF(BTRIM(currency), ''), 'INR')) AS currency,
                   SUM(current_balance) AS total_amount,
                   COUNT(*) AS record_count,
                   CURRENT_DATE AS snapshot_date
            FROM loans
            WHERE user_id = ? AND start_date <= ?
            -- Group by the same normalized values returned to Java. Otherwise NULL, blank,
            -- or whitespace-only statuses could appear as multiple rows all labelled 'unknown'.
            GROUP BY COALESCE(NULLIF(BTRIM(status), ''), 'unknown'),
                     UPPER(COALESCE(NULLIF(BTRIM(currency), ''), 'INR'))
            ORDER BY currency, category
            """;

    /*
     * Translator 1:
     * Takes each summary row returned by the database (Query 1 & Query 3) and
     * packages it into a clean Java object (FinancialReportRow) for the app to use.
     */
    private static final RowMapper<FinancialReportRow> REPORT_ROW_MAPPER = (rs, row) -> new FinancialReportRow(
            rs.getString("metric"),
            rs.getString("category"),
            rs.getString("currency"),
            rs.getBigDecimal("total_amount"),
            rs.getLong("record_count"),
            rs.getObject("snapshot_date", LocalDate.class));

    /*
     * Translator 2:
     * Takes each monthly chart row returned by the database (Query 2) and
     * packages it into a clean Java object (FinancialTrendRow) for the charts.
     */
    private static final RowMapper<FinancialTrendRow> TREND_ROW_MAPPER = (rs, row) -> new FinancialTrendRow(
            rs.getString("period"),
            rs.getString("metric"),
            rs.getString("currency"),
            rs.getBigDecimal("total_amount"),
            rs.getLong("record_count"));

    // Spring's built-in tool for running SQL queries safely against the database
    private final JdbcTemplate jdbcTemplate;

    public FinancialReportingRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Generates the full dashboard summary for a user between two dates.
     *
     * <p>How it executes:
     * <ol>
     *   <li>Runs the shared source query with the user's ID six times (once per
     *       source table), then supplies from and to for the outer date filter.</li>
     *   <li>Runs Query 3 (LOAN_BALANCE_SQL) using (userId, to) to get the currently
     *       stored loan balances for loans started by the end date. These are current
     *       balances, not reconstructed historical balances, and are appended to the result.
     *       Each loan row includes snapshotDate (PostgreSQL CURRENT_DATE) so clients can
     *       distinguish today's balance from activity inside the requested period.</li>
     * </ol>
     * </p>
     */
    public List<FinancialReportRow> summarize(UUID userId, LocalDate from, LocalDate to) {
        List<FinancialReportRow> rows = jdbcTemplate.query(
                REPORT_SQL,
                REPORT_ROW_MAPPER,
                userId,
                userId,
                userId,
                userId,
                userId,
                userId, // One owner ID for each source table.
                from,
                to); // Inclusive date range applied after the source rows are combined.
        rows.addAll(jdbcTemplate.query(LOAN_BALANCE_SQL, REPORT_ROW_MAPPER, userId, to));
        return rows;
    }

    /**
     * Generates the month-by-month chart numbers for a user between two dates.
     *
     * <p>The same shared source query is used by the summary and trend operations.
     * We pass the authenticated user ID six times (one per source table), followed
     * by from and to for the date range.</p>
     */
    public List<FinancialTrendRow> monthlyTrends(UUID userId, LocalDate from, LocalDate to) {
        return jdbcTemplate.query(
                MONTHLY_TREND_SQL,
                TREND_ROW_MAPPER,
                userId,
                userId,
                userId,
                userId,
                userId,
                userId, // One owner ID for each source table.
                from,
                to); // Inclusive date range applied after the source rows are combined.
    }
}
