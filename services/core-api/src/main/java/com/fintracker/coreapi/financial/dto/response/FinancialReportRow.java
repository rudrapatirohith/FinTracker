package com.fintracker.coreapi.financial.dto.response;

import java.math.BigDecimal;

/**
 * Currency-safe aggregate for one financial metric and category.
 * Amounts from different currencies are deliberately never combined.
 */
public record FinancialReportRow(
        String metric,
        String category,
        String currency,
        BigDecimal totalAmount,
        long recordCount) {}
