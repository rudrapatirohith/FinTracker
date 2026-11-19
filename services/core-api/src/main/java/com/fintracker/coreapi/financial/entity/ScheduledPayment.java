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
@Table(name = "scheduled_payments")
public class ScheduledPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "payment_name", nullable = false, columnDefinition = "text")
    private String paymentName;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(columnDefinition = "text")
    private String currency = "USD";

    @Column(columnDefinition = "text")
    private String category;

    @Column(columnDefinition = "text")
    private String recipient;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(columnDefinition = "text")
    private String frequency;

    @Column(name = "is_recurring")
    private Boolean recurring = false;

    @Column(name = "auto_pay")
    private Boolean autoPay = false;

    @Column(columnDefinition = "text")
    private String status = "pending";

    @Column(name = "reminder_days")
    private Integer reminderDays = 3;

    @Column(columnDefinition = "text")
    private String notes;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    protected ScheduledPayment() {
    }

    public ScheduledPayment(
            UUID userId,
            String paymentName,
            BigDecimal amount,
            LocalDate dueDate
    ) {
        this.userId = userId;
        this.paymentName = paymentName;
        this.amount = amount;
        this.dueDate = dueDate;
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
    public String getPaymentName() { return paymentName; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public String getCategory() { return category; }
    public String getRecipient() { return recipient; }
    public LocalDate getDueDate() { return dueDate; }
    public String getFrequency() { return frequency; }
    public Boolean getRecurring() { return recurring; }
    public Boolean getAutoPay() { return autoPay; }
    public String getStatus() { return status; }
    public Integer getReminderDays() { return reminderDays; }
    public String getNotes() { return notes; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
