package com.fintracker.coreapi.financial.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * One line in the dashboard/report result sent back to the frontend.
 *
 * <p>Think of this as one line in a financial summary table. For example,
 * metric=INCOME, category=Salary, currency=USD, totalAmount=5000.00,
 * recordCount=2 means the database found two Salary income records whose
 * amounts add up to USD 5,000.00. INR income appears in a different line;
 * this response never converts or adds INR to USD.</p>
 *
 * <p>This is a response-only object: it carries calculated results from the
 * repository to the API caller. It does not read or change database records.
 * The snapshotDate field is populated only for LOAN_OUTSTANDING rows because
 * those rows show the current database balance, not a historical period balance.
 * It is null for activity rows that belong to the requested date range.</p>
 *
 * @param metric identifies the source/workflow, such as INCOME or LOAN_PAYMENT
 * @param category category label, or a fixed label for sources without categories
 * @param currency currency code for totalAmount
 * @param totalAmount sum of amounts in this metric/category/currency group
 * @param recordCount number of source records included in the sum
 * @param snapshotDate database date on which a current snapshot was read; null for period activity
 */
public record FinancialReportRow(
        String metric,
        String category,
        String currency,
        BigDecimal totalAmount,
        long recordCount,
        LocalDate snapshotDate) {}
