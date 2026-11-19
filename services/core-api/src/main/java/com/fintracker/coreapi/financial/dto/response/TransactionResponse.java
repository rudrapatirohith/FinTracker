package com.fintracker.coreapi.financial.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.fintracker.coreapi.financial.entity.FinancialTransaction;

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
        Instant updatedAt
) {
    public TransactionResponse {
        tags = tags == null ? null : tags.clone();
    }

    @Override
    public String[] tags() {
        return tags == null ? null : tags.clone();
    }

    public static TransactionResponse from(FinancialTransaction entity) {
        return new TransactionResponse(
                entity.getId(), entity.getType(), entity.getCategory(),
                entity.getSubcategory(), entity.getAmount(), entity.getCurrency(),
                entity.getDescription(), entity.getMerchant(), entity.getTransactionDate(),
                entity.getPaymentMethod(), entity.getReferenceId(), entity.getTags(),
                entity.getCreatedAt(), entity.getUpdatedAt()
        );
    }
}
