/**
 * Public monthly-expense response shape.
 * The mapper exposes business fields and audit timestamps while deliberately omitting the internal owner UUID, keeping tenant implementation details out of the API.
 */
package com.fintracker.coreapi.financial.dto.response;

import com.fintracker.coreapi.financial.entity.MonthlyExpense;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** API representation that does not expose the internal owner UUID. */
public record MonthlyExpenseResponse(
        UUID id,
        String expenseName,
        BigDecimal amount,
        String currency,
        BigDecimal exchangeRate,
        BigDecimal inrEquivalent,
        String category,
        LocalDate expenseDate,
        String paymentMethod,
        String notes,
        Instant createdAt,
        Instant updatedAt) {
    public static MonthlyExpenseResponse from(MonthlyExpense entity) {
        return new MonthlyExpenseResponse(
                entity.getId(),
                entity.getExpenseName(),
                entity.getAmount(),
                entity.getCurrency(),
                entity.getExchangeRate(),
                entity.getInrEquivalent(),
                entity.getCategory(),
                entity.getExpenseDate(),
                entity.getPaymentMethod(),
                entity.getNotes(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }
}
