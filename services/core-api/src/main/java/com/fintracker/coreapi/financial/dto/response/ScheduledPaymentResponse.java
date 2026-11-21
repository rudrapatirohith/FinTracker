/**
 * Public response projection for a scheduled payment.
 * It returns the obligation details and audit timestamps needed by the client without exposing the internal tenant key.
 * The static mapper is the single place where the persistence entity is translated into the API contract.
 */
package com.fintracker.coreapi.financial.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.fintracker.coreapi.financial.entity.ScheduledPayment;

/** API representation excluding internal tenant metadata. */
public record ScheduledPaymentResponse(
        UUID id,
        String paymentName,
        BigDecimal amount,
        String currency,
        String category,
        String recipient,
        LocalDate dueDate,
        String frequency,
        Boolean recurring,
        Boolean autoPay,
        String status,
        Integer reminderDays,
        String notes,
        Instant createdAt,
        Instant updatedAt
) {
    public static ScheduledPaymentResponse from(ScheduledPayment entity) {
        return new ScheduledPaymentResponse(
                entity.getId(), entity.getPaymentName(), entity.getAmount(),
                entity.getCurrency(), entity.getCategory(), entity.getRecipient(),
                entity.getDueDate(), entity.getFrequency(), entity.getRecurring(),
                entity.getAutoPay(), entity.getStatus(), entity.getReminderDays(),
                entity.getNotes(), entity.getCreatedAt(), entity.getUpdatedAt()
        );
    }
}
