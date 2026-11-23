package com.fintracker.coreapi.financial.dto.response;

import java.math.BigDecimal;

/**
 * One aggregated result for a month, financial metric, and currency.
 *
 * <p>Example: period=2026-01, metric=INCOME, currency=USD,
 * totalAmount=5000.00 represents USD income recorded in January 2026.
 * Other currencies and metrics are returned as separate rows.</p>
 *
 * @param period calendar month formatted as YYYY-MM
 * @param metric financial source/workflow being measured
 * @param currency currency code for totalAmount
 * @param totalAmount sum of amounts in this month/metric/currency group
 * @param recordCount number of source records included in the sum
 */
public record FinancialTrendRow(
        String period,
        String metric,
        String currency,
        BigDecimal totalAmount,
        long recordCount) {}
