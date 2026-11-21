package com.fintracker.coreapi.financial.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * JPA mapping for the transactions table.
 * This class represents persisted transaction state, including the tenant owner and PostgreSQL text-array tags.
 * Ownership is immutable through the public update method; tags are copied on both input and output to prevent mutation outside the entity.
 * Lifecycle callbacks maintain audit timestamps.
 */
@Entity
@Table(name = "transactions")
public class FinancialTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, columnDefinition = "text")
    private String type;

    @Column(nullable = false, columnDefinition = "text")
    private String category;

    @Column(columnDefinition = "text")
    private String subcategory;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(columnDefinition = "text")
    private String currency = "USD";

    @Column(columnDefinition = "text")
    private String description;

    @Column(columnDefinition = "text")
    private String merchant;

    @Column(name = "transaction_date", nullable = false)
    private LocalDate transactionDate;

    @Column(name = "payment_method", columnDefinition = "text")
    private String paymentMethod;

    @Column(name = "reference_id")
    private UUID referenceId;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(columnDefinition = "text[]")
    private String[] tags;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    protected FinancialTransaction() {}

    public FinancialTransaction(
            UUID userId, String type, String category, BigDecimal amount, LocalDate transactionDate) {
        this.userId = userId;
        this.type = type;
        this.category = category;
        this.amount = amount;
        this.transactionDate = transactionDate;
    }

    @PrePersist
    /**
     * Initializes createdAt and updatedAt immediately before the first insert; existing non-null timestamp values are preserved.
     */
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    /**
     * Refreshes updatedAt immediately before an entity update so the audit timestamp reflects the latest persisted change.
     */
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getType() {
        return type;
    }

    public String getCategory() {
        return category;
    }

    public String getSubcategory() {
        return subcategory;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public String getDescription() {
        return description;
    }

    public String getMerchant() {
        return merchant;
    }

    public LocalDate getTransactionDate() {
        return transactionDate;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public UUID getReferenceId() {
        return referenceId;
    }

    public String[] getTags() {
        return tags == null ? null : tags.clone();
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    /**
     * Applies a complete replacement of mutable transaction fields.
     * The owning user and database identity are intentionally immutable here.
     */
    public void updateDetails(
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
            String[] tags) {
        this.type = type;
        this.category = category;
        this.subcategory = subcategory;
        this.amount = amount;
        this.currency = currency;
        this.description = description;
        this.merchant = merchant;
        this.transactionDate = transactionDate;
        this.paymentMethod = paymentMethod;
        this.referenceId = referenceId;
        this.tags = tags == null ? null : tags.clone();
    }
}
