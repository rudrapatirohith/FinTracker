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
 *   <li><b>Exact date window:</b> Only counts money between your chosen Start Date and End Date.</li>
 *   <li><b>Never mixes currencies:</b> Dollars (USD), Rupees (INR), and Canadian Dollars (CAD) are kept in separate totals—never added together.</li>
 *   <li><b>No double-counting:</b> Special records (like Monthly Expenses) and General Transactions get different labels so the same purchase is never counted twice.</li>
 * </ul>
 * </p>
 */
@Repository
public class FinancialReportingRepository {

    /*
     * =========================================================================
     * QUERY 1: DASHBOARD TOTALS BY CATEGORY (REPORT_SQL)
     * =========================================================================
     * What it does:
     * Calculates how much money came in and went out between two dates, broken
     * down by category (e.g., Salary, Rent, Loan Payments) and currency.
     *
     * How it works in 2 simple steps:
     * Step 1 (Inside the brackets): Collects matching receipts from 6 separate
     *        database tables and stacks them into one combined pile ("UNION ALL").
     *        Even if two receipts have the exact same amount on the same day,
     *        both are kept so nothing is lost.
     * Step 2 (Top & bottom lines): Groups that big pile by Type, Category, and
     *        Currency, then adds up the dollar/rupee amounts ("SUM") and counts
     *        how many receipts were in each pile ("COUNT").
     *
     * Note on the "?" symbols:
     * Each "?" is a safe placeholder where Java plugs in your User ID, Start Date,
     * and End Date right before asking the database for the numbers.
     */
    private static final String REPORT_SQL = """
            SELECT metric, category, currency, SUM(amount) AS total_amount, COUNT(*) AS record_count
            FROM (
                -- Pile 1: Money earned from the dedicated Income table.
                -- If a category is blank, label it 'Uncategorized'. If currency is blank, assume 'USD'.
                SELECT 'INCOME' AS metric,
                       COALESCE(NULLIF(BTRIM(category), ''), 'Uncategorized') AS category,
                       UPPER(COALESCE(NULLIF(BTRIM(currency), ''), 'USD')) AS currency,
                       amount
                FROM income
                WHERE user_id = ? AND date >= ? AND date <= ?

                UNION ALL

                -- Pile 2: Bills and spending from the Monthly Expenses table.
                -- Labeled 'MONTHLY_EXPENSE' so it stays separate from general transactions below.
                SELECT 'MONTHLY_EXPENSE' AS metric,
                       COALESCE(NULLIF(BTRIM(category), ''), 'Uncategorized') AS category,
                       UPPER(COALESCE(NULLIF(BTRIM(currency), ''), 'INR')) AS currency,
                       amount
                FROM monthly_expenses
                WHERE user_id = ? AND expense_date >= ? AND expense_date <= ?

                UNION ALL

                -- Pile 3: Actual loan payments made during this period.
                -- Note: This counts the monthly payment made, NOT the total loan balance still owed.
                SELECT 'LOAN_PAYMENT' AS metric, 'Loan Payments' AS category,
                       UPPER(COALESCE(NULLIF(BTRIM(currency), ''), 'INR')) AS currency,
                       amount
                FROM loan_payments
                WHERE user_id = ? AND payment_date >= ? AND payment_date <= ?

                UNION ALL

                -- Pile 4: International money transfers sent out.
                -- We only count the money SENT (amount_sent), ignoring the converted amount
                -- received on the other side so the same transfer isn't counted twice.
                SELECT 'TRANSFER' AS metric, 'Transfers' AS category,
                       UPPER(COALESCE(NULLIF(BTRIM(currency_sent), ''), 'USD')) AS currency,
                       amount_sent AS amount
                FROM international_transfers
                WHERE user_id = ? AND transfer_date >= ? AND transfer_date <= ?

                UNION ALL

                -- Pile 5: Scheduled bills that have actually been PAID.
                -- Unpaid, upcoming, or cancelled bills are ignored here.
                SELECT 'SCHEDULED_PAYMENT' AS metric,
                       COALESCE(NULLIF(BTRIM(category), ''), 'Uncategorized') AS category,
                       UPPER(COALESCE(NULLIF(BTRIM(currency), ''), 'USD')) AS currency,
                       amount
                FROM scheduled_payments
                WHERE user_id = ? AND status = 'paid' AND due_date >= ? AND due_date <= ?

                UNION ALL

                -- Pile 6: General day-to-day entries from the main Transactions table.
                -- Labeled 'TRANSACTION_INCOME' or 'TRANSACTION_EXPENSE' to keep them separate
                -- from the dedicated Income and Monthly Expenses piles above.
                SELECT 'TRANSACTION_' || UPPER(type) AS metric,
                       COALESCE(NULLIF(BTRIM(category), ''), 'Uncategorized') AS category,
                       UPPER(COALESCE(NULLIF(BTRIM(currency), ''), 'USD')) AS currency,
                       amount
                FROM transactions
                WHERE user_id = ? AND transaction_date >= ? AND transaction_date <= ?
                  AND UPPER(type) IN ('INCOME', 'EXPENSE')
            ) financial_rows
            -- Combine matching piles and sort the final list alphabetically
            GROUP BY metric, category, currency
            ORDER BY metric, currency, category
            """;

    /*
     * =========================================================================
     * QUERY 2: MONTH-BY-MONTH CHART DATA (MONTHLY_TREND_SQL)
     * =========================================================================
     * What it does:
     * Prepares numbers for your dashboard's bar and line charts over time.
     *
     * How it differs from Query 1:
     * Instead of grouping by Category (Rent, Food, Salary), it groups every
     * receipt by the Calendar Month it happened in (for example, '2026-01' for
     * January 2026). That way, the chart can draw one bar per month while still
     * keeping Income, Expenses, and different currencies completely separate.
     */
    private static final String MONTHLY_TREND_SQL = """
            SELECT TO_CHAR(DATE_TRUNC('month', event_date), 'YYYY-MM') AS period,
                   metric, currency, SUM(amount) AS total_amount, COUNT(*) AS record_count
            FROM (
                -- 1. Income grouped by the month it was received
                SELECT date AS event_date, 'INCOME' AS metric,
                       UPPER(COALESCE(NULLIF(BTRIM(currency), ''), 'USD')) AS currency, amount
                FROM income
                WHERE user_id = ? AND date >= ? AND date <= ?

                UNION ALL

                -- 2. Monthly expenses grouped by the month they occurred
                SELECT expense_date AS event_date, 'MONTHLY_EXPENSE' AS metric,
                       UPPER(COALESCE(NULLIF(BTRIM(currency), ''), 'INR')) AS currency, amount
                FROM monthly_expenses
                WHERE user_id = ? AND expense_date >= ? AND expense_date <= ?

                UNION ALL

                -- 3. Loan payments grouped by the month they were paid
                SELECT payment_date AS event_date, 'LOAN_PAYMENT' AS metric,
                       UPPER(COALESCE(NULLIF(BTRIM(currency), ''), 'INR')) AS currency, amount
                FROM loan_payments
                WHERE user_id = ? AND payment_date >= ? AND payment_date <= ?

                UNION ALL

                -- 4. International transfers (sent amount only) grouped by transfer month
                SELECT transfer_date AS event_date, 'TRANSFER' AS metric,
                       UPPER(COALESCE(NULLIF(BTRIM(currency_sent), ''), 'USD')) AS currency, amount_sent
                FROM international_transfers
                WHERE user_id = ? AND transfer_date >= ? AND transfer_date <= ?

                UNION ALL

                -- 5. Paid scheduled bills grouped by the month they were due
                SELECT due_date AS event_date, 'SCHEDULED_PAYMENT' AS metric,
                       UPPER(COALESCE(NULLIF(BTRIM(currency), ''), 'USD')) AS currency, amount
                FROM scheduled_payments
                WHERE user_id = ? AND status = 'paid' AND due_date >= ? AND due_date <= ?

                UNION ALL

                -- 6. General income/expense transactions grouped by transaction month
                SELECT transaction_date AS event_date, 'TRANSACTION_' || UPPER(type) AS metric,
                       UPPER(COALESCE(NULLIF(BTRIM(currency), ''), 'USD')) AS currency, amount
                FROM transactions
                WHERE user_id = ? AND transaction_date >= ? AND transaction_date <= ?
                  AND UPPER(type) IN ('INCOME', 'EXPENSE')
            ) financial_events
            -- Group all dates within the same month together (e.g., Jan 3 and Jan 25 both become '2026-01')
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
     * report for last year, this still shows today's remaining balance, not what
     * the balance was back then.
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
     * Translator 1:
     * Takes each summary row returned by the database (Query 1 & Query 3) and
     * packages it into a clean Java object (FinancialReportRow) for the app to use.
     */
    private static final RowMapper<FinancialReportRow> REPORT_ROW_MAPPER =
            (rs, row) -> new FinancialReportRow(
                    rs.getString("metric"),
                    rs.getString("category"),
                    rs.getString("currency"),
                    rs.getBigDecimal("total_amount"),
                    rs.getLong("record_count"));

    /*
     * Translator 2:
     * Takes each monthly chart row returned by the database (Query 2) and
     * packages it into a clean Java object (FinancialTrendRow) for the charts.
     */
    private static final RowMapper<FinancialTrendRow> TREND_ROW_MAPPER =
            (rs, row) -> new FinancialTrendRow(
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
     *   <li>Runs Query 1 (REPORT_SQL). Because Query 1 combines 6 tables, we pass
     *       (userId, from, to) 6 times—once for each table's "?" placeholders.</li>
     *   <li>Runs Query 3 (LOAN_BALANCE_SQL) using (userId, to) to get current loan
     *       debt, and adds those rows onto the bottom of the same list.</li>
     * </ol>
     * </p>
     */
    public List<FinancialReportRow> summarize(UUID userId, LocalDate from, LocalDate to) {
        List<FinancialReportRow> rows = jdbcTemplate.query(
                REPORT_SQL,
                REPORT_ROW_MAPPER,
                userId, from, to, // Fills "?" for Pile 1: income
                userId, from, to, // Fills "?" for Pile 2: monthly_expenses
                userId, from, to, // Fills "?" for Pile 3: loan_payments
                userId, from, to, // Fills "?" for Pile 4: international_transfers
                userId, from, to, // Fills "?" for Pile 5: scheduled_payments
                userId, from, to); // Fills "?" for Pile 6: transactions
        rows.addAll(jdbcTemplate.query(LOAN_BALANCE_SQL, REPORT_ROW_MAPPER, userId, to));
        return rows;
    }

    /**
     * Generates the month-by-month chart numbers for a user between two dates.
     *
     * <p>Just like {@code summarize()}, we pass (userId, from, to) 6 times so each
     * of the 6 tables in Query 2 (MONTHLY_TREND_SQL) filters for the right user
     * and date window.</p>
     */
    public List<FinancialTrendRow> monthlyTrends(UUID userId, LocalDate from, LocalDate to) {
        return jdbcTemplate.query(
                MONTHLY_TREND_SQL,
                TREND_ROW_MAPPER,
                userId, from, to, // Fills "?" for Table 1: income
                userId, from, to, // Fills "?" for Table 2: monthly_expenses
                userId, from, to, // Fills "?" for Table 3: loan_payments
                userId, from, to, // Fills "?" for Table 4: international_transfers
                userId, from, to, // Fills "?" for Table 5: scheduled_payments
                userId, from, to); // Fills "?" for Table 6: transactions
    }
}