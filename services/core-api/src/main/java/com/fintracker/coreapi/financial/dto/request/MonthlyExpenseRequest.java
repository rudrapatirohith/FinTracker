package com.fintracker.coreapi.financial.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Validated replacement payload for a monthly expense record. */
public record MonthlyExpenseRequest(
        @NotBlank @Size(max = 255) String expenseName,

        @NotNull @DecimalMin("0.01") @Digits(integer = 13, fraction = 2)
        BigDecimal amount,

        @Pattern(regexp = "[A-Za-z]{3}") String currency,

        @DecimalMin("0.000001") @Digits(integer = 4, fraction = 6)
        BigDecimal exchangeRate,

        @DecimalMin("0.00") @Digits(integer = 13, fraction = 2)
        BigDecimal inrEquivalent,

        @Size(max = 100) String category,
        @NotNull LocalDate expenseDate,
        @Size(max = 100) String paymentMethod,
        @Size(max = 2000) String notes) {}
