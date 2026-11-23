package com.fintracker.coreapi.financial.repository;

import com.fintracker.coreapi.financial.dto.response.FinancialReportRow;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/**
 * Read model for financial reporting. Aggregation is performed in PostgreSQL
 * and every source query is restricted to the authenticated owner and date range.
 *
 * Income, monthly expenses, and generic transactions remain distinct metrics:
 * the same real-world expense may be represented in both expense workflows,
 * so combining them would risk double counting.
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

    private static final RowMapper<FinancialReportRow> ROW_MAPPER = FinancialReportingRepository::mapRow;

    private final JdbcTemplate jdbcTemplate;

    public FinancialReportingRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<FinancialReportRow> summarize(UUID userId, LocalDate from, LocalDate to) {
        return jdbcTemplate.query(
                REPORT_SQL,
                ROW_MAPPER,
                userId, from, to,
                userId, from, to,
                userId, from, to);
    }

    private static FinancialReportRow mapRow(ResultSet resultSet, int rowNumber) throws SQLException {
        return new FinancialReportRow(
                resultSet.getString("metric"),
                resultSet.getString("category"),
                resultSet.getString("currency"),
                resultSet.getBigDecimal("total_amount"),
                resultSet.getLong("record_count"));
    }
}
