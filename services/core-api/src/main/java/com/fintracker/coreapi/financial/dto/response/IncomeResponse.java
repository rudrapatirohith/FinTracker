package com.fintracker.coreapi.financial.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.fintracker.coreapi.financial.entity.Income;

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
        BigDecimal inrEquivalent
) {
    public static IncomeResponse from(Income income) {
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
                income.getInrEquivalent()
        );
    }
}