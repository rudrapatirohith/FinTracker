package com.fintracker.coreapi.financial.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** Validated client input; user ownership is always derived from the JWT. */
public record TransactionRequest(
        @NotBlank @Size(max = 100) String type,
        @NotBlank @Size(max = 150) String category,
        @Size(max = 150) String subcategory,

        @NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2)
        BigDecimal amount,

        @Size(min = 3, max = 3) String currency,
        @Size(max = 2000) String description,
        @Size(max = 255) String merchant,
        @NotNull LocalDate transactionDate,
        @Size(max = 100) String paymentMethod,
        UUID referenceId,
        @Size(max = 50) String[] tags) {
    public TransactionRequest {
        tags = tags == null ? null : tags.clone();
    }

    /**
     * Returns a defensive copy of the tags array; the caller can modify the returned array without changing the request record.
     */
    @Override
    public String[] tags() {
        return tags == null ? null : tags.clone();
    }
}
