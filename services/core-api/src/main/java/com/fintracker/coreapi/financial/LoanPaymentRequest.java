package com.fintracker.coreapi.financial;

import com.fintracker.coreapi.financial.entity.*;
import com.fintracker.coreapi.financial.repository.*;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

/**
 * Validated input contract for a loan payment. Bean Validation enforces the declared amount, date, currency, length, and precision constraints; the route supplies the loan ID and the authenticated security context supplies the owner, so neither is accepted from request JSON.
 */
public record LoanPaymentRequest(
        @NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2)
        BigDecimal amount,

        @DecimalMin("0.00") @Digits(integer = 10, fraction = 2)
        BigDecimal principalAmount,

        @DecimalMin("0.00") @Digits(integer = 10, fraction = 2)
        BigDecimal interestAmount,

        @NotNull LocalDate paymentDate,
        @Size(max = 100) String paymentMethod,
        @Size(max = 2000) String notes,
        @NotBlank @Pattern(regexp = "INR|USD") String currency,

        @DecimalMin("0.000001") @Digits(integer = 4, fraction = 6)
        BigDecimal exchangeRate,

        @DecimalMin("0.00") @Digits(integer = 13, fraction = 2)
        BigDecimal inrEquivalent) {}
