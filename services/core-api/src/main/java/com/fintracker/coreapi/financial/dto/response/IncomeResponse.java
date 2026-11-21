package com.fintracker.coreapi.financial.dto.response;

import com.fintracker.coreapi.financial.entity.Income;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Creates this domain exception with the stable, client-safe not-found message used by the global exception handler.
 */
/**
 * Public response contract for an income record, including business fields, conversion metadata, and audit timestamps but excluding the internal owner UUID.
 */
public record IncomeResponse(
        UUID id,
        String source,
        BigDecimal amount,
        String currency,
        String category,
        String description,
        LocalDate date,
        Boolean recurring,
        String recurringFrequency,
        Instant createdAt,
        Instant updatedAt,
        BigDecimal exchangeRate,
        BigDecimal inrEquivalent) {
    /**
     * Maps persisted entity fields into the public response record; keeping this allowlist explicit prevents internal ownership and JPA state from leaking into JSON.
     */
    public static IncomeResponse from(Income income) {
        /**
         * Creates this domain exception with the stable, client-safe not-found message used by the global exception handler.
         */
        return new IncomeResponse(
                income.getId(),
                income.getSource(),
                income.getAmount(),
                income.getCurrency(),
                income.getCategory(),
                income.getDescription(),
                income.getDate(),
                income.getRecurring(),
                income.getRecurringFrequency(),
                income.getCreatedAt(),
                income.getUpdatedAt(),
                income.getExchangeRate(),
                income.getInrEquivalent());
    }
}
