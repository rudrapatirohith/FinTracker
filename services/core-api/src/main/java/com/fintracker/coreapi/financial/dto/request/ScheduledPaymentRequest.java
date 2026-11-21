/**
 * Validated request for a scheduled obligation.
 * Required name, amount, and due date identify the payment; optional recurrence, autopay, status, and reminder fields are defaulted by the service when omitted.
 * Reminder-day bounds prevent invalid or unreasonable values from reaching persistence.
 */
package com.fintracker.coreapi.financial.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Validated input for creating or replacing a scheduled payment. */
public record ScheduledPaymentRequest(
        @NotBlank @Size(max = 255) String paymentName,
        @NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) BigDecimal amount,
        @Pattern(regexp = "[A-Za-z]{3}") String currency,
        @Size(max = 100) String category,
        @Size(max = 255) String recipient,
        @NotNull LocalDate dueDate,
        @Size(max = 50) String frequency,
        Boolean recurring,
        Boolean autoPay,
        @Size(max = 50) String status,
        @Min(0) @Max(365) Integer reminderDays,
        @Size(max = 2000) String notes
) {
}
