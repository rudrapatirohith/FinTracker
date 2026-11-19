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
@Table(name = "monthly_expenses")
public class MonthlyExpense {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "expense_name", nullable = false, length = 255)
    private String expenseName;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency = "INR";

    @Column(name = "exchange_rate", precision = 10, scale = 6)
    private BigDecimal exchangeRate;

    @Column(name = "inr_equivalent", precision = 15, scale = 2)
    private BigDecimal inrEquivalent;

    @Column(length = 100)
    private String category;

    @Column(name = "expense_date", nullable = false)
    private LocalDate expenseDate;

    @Column(name = "payment_method", length = 100)
    private String paymentMethod;

    @Column(columnDefinition = "text")
    private String notes;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    protected MonthlyExpense() {
    }

    public MonthlyExpense(
            UUID userId,
            String expenseName,
            BigDecimal amount,
            LocalDate expenseDate
    ) {
        this.userId = userId;
        this.expenseName = expenseName;
        this.amount = amount;
        this.expenseDate = expenseDate;
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
    public String getExpenseName() { return expenseName; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public BigDecimal getExchangeRate() { return exchangeRate; }
    public BigDecimal getInrEquivalent() { return inrEquivalent; }
    public String getCategory() { return category; }
    public LocalDate getExpenseDate() { return expenseDate; }
    public String getPaymentMethod() { return paymentMethod; }
    public String getNotes() { return notes; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    /** Updates mutable expense fields without allowing reassignment of its owner. */
    public void updateDetails(
            String expenseName,
            BigDecimal amount,
            String currency,
            BigDecimal exchangeRate,
            BigDecimal inrEquivalent,
            String category,
            LocalDate expenseDate,
            String paymentMethod,
            String notes
    ) {
        this.expenseName = expenseName;
        this.amount = amount;
        this.currency = currency;
        this.exchangeRate = exchangeRate;
        this.inrEquivalent = inrEquivalent;
        this.category = category;
        this.expenseDate = expenseDate;
        this.paymentMethod = paymentMethod;
        this.notes = notes;
    }
}
