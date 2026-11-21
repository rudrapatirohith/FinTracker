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
 * Validated input contract for an international transfer. It preserves the sent and received amounts/currencies plus exchange-rate and optional conversion metadata; Bean Validation enforces required values, supported currency codes, lengths, and numeric precision.
 */
public record TransferRequest(
        @NotBlank @Size(max = 255) String recipientName,
        @NotBlank @Size(max = 255) String recipientCountry,

        @NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2)
        BigDecimal amountSent,

        @NotBlank @Pattern(regexp = "INR|USD|EUR|GBP|CAD|AUD")
        String currencySent,

        @NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2)
        BigDecimal amountReceived,

        @NotBlank @Pattern(regexp = "INR|USD|EUR|GBP|CAD|AUD")
        String currencyReceived,

        @NotNull @DecimalMin("0.000001") @Digits(integer = 4, fraction = 6)
        BigDecimal exchangeRate,

        @DecimalMin("0.00") @Digits(integer = 10, fraction = 2)
        BigDecimal transferFee,

        @Size(max = 100) String transferMethod,
        @Size(max = 2000) String purpose,
        @NotBlank @Size(max = 50) String status,
        @NotNull LocalDate transferDate,
        LocalDate completionDate,
        @Size(max = 255) String referenceNumber,

        @DecimalMin("0.000001") @Digits(integer = 4, fraction = 6)
        BigDecimal usdToInrRate,

        @DecimalMin("0.00") @Digits(integer = 13, fraction = 2)
        BigDecimal inrEquivalent) {}
