package com.fintracker.coreapi.financial.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Validated input contract for loan terms. Field-level constraints cover required values and supported precision; LoanApiService additionally enforces the cross-field rule that an optional end date cannot precede the start date. Ownership is never client-supplied.
 */
public record LoanRequest(
        @NotBlank @Size(max = 255) String loanName,

        @NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2)
        BigDecimal principalAmount,

        @NotNull @DecimalMin("0.00") @Digits(integer = 10, fraction = 2)
        BigDecimal currentBalance,

        @DecimalMin("0.00") @Digits(integer = 3, fraction = 2)
        BigDecimal interestRate,

        @DecimalMin("0.00") @Digits(integer = 10, fraction = 2)
        BigDecimal monthlyPayment,

        @NotNull LocalDate startDate,
        LocalDate endDate,
        @Size(max = 255) String lender,
        @Size(max = 100) String loanType,

        @NotBlank @Pattern(regexp = "active|paid|closed|defaulted")
        String status,

        @NotBlank @Pattern(regexp = "INR|USD") String currency) {}
