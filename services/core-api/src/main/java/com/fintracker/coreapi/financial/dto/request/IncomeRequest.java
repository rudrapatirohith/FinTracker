/**
 * Client-supplied income fields and their validation rules.
 * The amount and conversion fields use explicit decimal precision to match the database columns; source and date are required, while optional currency and recurring values are normalized or defaulted by IncomeService.
 * No owner ID is accepted from the client.
 */
package com.fintracker.coreapi.financial.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record IncomeRequest(

        @NotBlank(message = "Income source is required.")
        String source,

        @NotNull(message = "Income amount is required.")
        @DecimalMin(value = "0.01", message = "Income amount must be greater than zero.")
        @Digits(integer = 10, fraction = 2, message = "Income amount exceeds the supported precision.")
        BigDecimal amount,

        String currency,

        String category,

        String description,

        @NotNull(message = "Income date is required.")
        LocalDate date,

        Boolean recurring,

        String recurringFrequency,

        @DecimalMin(value = "0.000001", message = "Exchange rate must be positive.")
        @Digits(integer = 4, fraction = 6, message = "Exchange rate exceeds the supported precision.")
        BigDecimal exchangeRate,

        @DecimalMin(value = "0.00", message = "INR equivalent cannot be negative.")
        @Digits(integer = 13, fraction = 2, message = "INR equivalent exceeds the supported precision.")
        BigDecimal inrEquivalent
) {
}