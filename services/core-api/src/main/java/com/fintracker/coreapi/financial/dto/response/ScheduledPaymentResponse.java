package com.fintracker.coreapi.financial.dto.response;

import com.fintracker.coreapi.financial.entity.ScheduledPayment;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** API representation excluding internal tenant metadata. */
/**
 * Public response contract for a scheduled payment, exposing obligation details and audit timestamps without exposing the internal tenant key.
 */
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
        Instant updatedAt) {
    /**
     * Maps the persisted entity to the public response contract, explicitly selecting fields and excluding internal owner metadata.
     */
    public static ScheduledPaymentResponse from(ScheduledPayment entity) {
        return new ScheduledPaymentResponse(
                entity.getId(),
                entity.getPaymentName(),
                entity.getAmount(),
                entity.getCurrency(),
                entity.getCategory(),
                entity.getRecipient(),
                entity.getDueDate(),
                entity.getFrequency(),
                entity.getRecurring(),
                entity.getAutoPay(),
                entity.getStatus(),
                entity.getReminderDays(),
                entity.getNotes(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }
}
