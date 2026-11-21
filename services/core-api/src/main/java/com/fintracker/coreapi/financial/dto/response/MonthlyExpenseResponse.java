package com.fintracker.coreapi.financial.dto.response;

import com.fintracker.coreapi.financial.entity.MonthlyExpense;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** API representation that does not expose the internal owner UUID. */
/**
 * Public response contract for a monthly expense, including its business fields and audit timestamps without exposing the internal owner UUID.
 */
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
    /**
     * Maps persisted entity fields into the public response record; keeping this allowlist explicit prevents internal ownership and JPA state from leaking into JSON.
     */
    public static MonthlyExpenseResponse from(MonthlyExpense entity) {
        /**
         * Creates this domain exception with the stable, client-safe not-found message used by the global exception handler.
         */
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
