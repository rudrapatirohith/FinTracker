package com.fintracker.coreapi.financial.dto.response;

import java.math.BigDecimal;

/**
 * One aggregated result returned by dashboard, category analytics, or reports.
 *
 * <p>Example: metric=INCOME, category=Salary, currency=USD,
 * totalAmount=5000.00, recordCount=2 means two Salary income records
 * added up to USD 5,000.00. A separate INR row is returned rather than
 * converting or adding it to the USD amount.</p>
 *
 * @param metric identifies the source/workflow, such as INCOME or LOAN_PAYMENT
 * @param category category label, or a fixed label for sources without categories
 * @param currency currency code for totalAmount
 * @param totalAmount sum of amounts in this metric/category/currency group
 * @param recordCount number of source records included in the sum
 */
public record FinancialReportRow(
        String metric,
        String category,
        String currency,
        BigDecimal totalAmount,
        long recordCount) {}
