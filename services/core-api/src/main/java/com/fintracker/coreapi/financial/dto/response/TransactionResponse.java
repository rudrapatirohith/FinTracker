package com.fintracker.coreapi.financial.dto.response;

import com.fintracker.coreapi.financial.entity.FinancialTransaction;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;


/**
 * Public response contract for a financial transaction. It excludes internal ownership metadata and returns a defensive copy of the tags array.
 */
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

/**
     * Returns a defensive copy of the tags array; the caller can modify the returned array without changing the request record.
     */
    @Override

    public String[] tags() {
        return tags == null ? null : tags.clone();
    }

    /**
     * Maps the persisted entity to the public response contract, explicitly selecting fields and excluding internal owner metadata.
     */
    public static TransactionResponse from(FinancialTransaction entity) {

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
