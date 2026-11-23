package com.fintracker.coreapi.financial.dto.response;

import java.math.BigDecimal;

/** Currency-safe monthly aggregate for one financial metric. */
public record FinancialTrendRow(
        String period,
        String metric,
        String currency,
        BigDecimal totalAmount,
        long recordCount) {}
