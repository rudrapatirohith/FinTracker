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
