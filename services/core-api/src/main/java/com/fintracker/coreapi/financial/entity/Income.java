package com.fintracker.coreapi.financial.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "income")
public class Income {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, columnDefinition = "text")
    private String source;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(columnDefinition = "text")
    private String currency = "USD";

    @Column(columnDefinition = "text")
    private String category;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "date", nullable = false)
    private LocalDate date;

    @Column(name = "is_recurring")
    private Boolean recurring = false;

    @Column(name = "recurring_frequency", columnDefinition = "text")
    private String recurringFrequency;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "exchange_rate", precision = 10, scale = 6)
    private BigDecimal exchangeRate;

    @Column(name = "inr_equivalent", precision = 15, scale = 2)
    private BigDecimal inrEquivalent;

    protected Income() {
    }

    public Income(
            UUID userId,
            String source,
            BigDecimal amount,
            LocalDate date
    ) {
        this.userId = userId;
        this.source = source;
        this.amount = amount;
        this.date = date;
    }

    @PrePersist
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
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getSource() { return source; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public String getCategory() { return category; }
    public String getDescription() { return description; }
    public LocalDate getDate() { return date; }
    public Boolean getRecurring() { return recurring; }
    public String getRecurringFrequency() { return recurringFrequency; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public BigDecimal getExchangeRate() { return exchangeRate; }
    public BigDecimal getInrEquivalent() { return inrEquivalent; }

    public void updateDetails(
            String source,
            BigDecimal amount,
            String currency,
            String category,
            String description,
            LocalDate date,
            Boolean recurring,
            String recurringFrequency,
            BigDecimal exchangeRate,
            BigDecimal inrEquivalent
    ) {
        this.source = source;
        this.amount = amount;
        this.currency = currency;
        this.category = category;
        this.description = description;
        this.date = date;
        this.recurring = recurring;
        this.recurringFrequency = recurringFrequency;
        this.exchangeRate = exchangeRate;
        this.inrEquivalent = inrEquivalent;
    }}