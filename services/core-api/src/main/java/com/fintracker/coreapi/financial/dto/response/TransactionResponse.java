package com.fintracker.coreapi.financial.dto.response;

import com.fintracker.coreapi.financial.entity.FinancialTransaction;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** API representation deliberately excludes the internal owner identifier. */
public record TransactionResponse(
        UUID id,
        String type,
        String category,
        String subcategory,
        BigDecimal amount,
        String currency,
        String description,
        String merchant,
        LocalDate transactionDate,
        String paymentMethod,
        UUID referenceId,
        String[] tags,
        Instant createdAt,
        Instant updatedAt) {
    public TransactionResponse {
        tags = tags == null ? null : tags.clone();
    }

    @Override
    /**
     * Returns a defensive copy of the tags array; the caller can modify the returned array without changing the request record.
     */
    public String[] tags() {
        return tags == null ? null : tags.clone();
    }

    /**
     * Maps the persisted entity to the public response contract, explicitly selecting fields and excluding internal owner metadata.
     */
    public static TransactionResponse from(FinancialTransaction entity) {
        /**
         * Defensively copies the tags array when the response record is constructed so the returned DTO does not retain a caller-owned mutable array.
         */
        return new TransactionResponse(
                entity.getId(),
                entity.getType(),
                entity.getCategory(),
                entity.getSubcategory(),
                entity.getAmount(),
                entity.getCurrency(),
                entity.getDescription(),
                entity.getMerchant(),
                entity.getTransactionDate(),
                entity.getPaymentMethod(),
                entity.getReferenceId(),
                entity.getTags(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }
}
