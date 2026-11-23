package com.fintracker.coreapi.financial.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Validated input contract for a loan payment. Bean Validation enforces the declared amount, date, optional currency, length, and precision constraints; the route supplies the loan ID and the authenticated security context supplies the owner, so neither is accepted from request JSON.
 */
public record LoanPaymentRequest(
        @NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2)
        BigDecimal amount,

        @NotNull @DecimalMin("0.00") @Digits(integer = 10, fraction = 2)
        BigDecimal principalAmount,

        @NotNull @DecimalMin("0.00") @Digits(integer = 10, fraction = 2)
        BigDecimal interestAmount,

        @NotNull LocalDate paymentDate,
        @Size(max = 100) String paymentMethod,
        @Size(max = 2000) String notes,
        @Pattern(regexp = "INR|USD") String currency,

        @DecimalMin("0.000001") @Digits(integer = 4, fraction = 6)
        BigDecimal exchangeRate,

        @DecimalMin("0.00") @Digits(integer = 13, fraction = 2)
        BigDecimal inrEquivalent) {

    /** Ensures the recorded payment total is exactly explained by principal and interest. */
    @AssertTrue(message = "amount must equal principalAmount plus interestAmount")
    public boolean isBreakdownValid() {
        return amount != null
                && principalAmount != null
                && interestAmount != null
                && amount.compareTo(principalAmount.add(interestAmount)) == 0;
    }
}
