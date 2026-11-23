package com.fintracker.coreapi.financial.dto.response;

import java.math.BigDecimal;

/**
 * One line of chart data for a particular month, financial activity, and currency.
 *
 * <p>Think of each object as one plotted value on a monthly chart. For example,
 * period=2026-01, metric=INCOME, currency=USD, totalAmount=5000.00 means the
 * database found USD 5,000.00 of income in January 2026. January INR income
 * and January expenses are separate lines/points, not mixed into this value.</p>
 *
 * <p>This object only carries the finished result to the frontend. PostgreSQL
 * calculates the monthly total; the frontend should display it, not recalculate it.</p>
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
