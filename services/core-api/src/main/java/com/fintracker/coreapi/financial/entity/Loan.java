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
@Table(name = "loans")
public class Loan {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "loan_name", nullable = false, columnDefinition = "text")
    private String loanName;

    @Column(name = "principal_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal principalAmount;

    @Column(name = "current_balance", nullable = false, precision = 12, scale = 2)
    private BigDecimal currentBalance;

    @Column(name = "interest_rate", precision = 5, scale = 2)
    private BigDecimal interestRate;

    @Column(name = "monthly_payment", precision = 12, scale = 2)
    private BigDecimal monthlyPayment;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(columnDefinition = "text")
    private String lender;

    @Column(name = "loan_type", columnDefinition = "text")
    private String loanType;

    @Column(columnDefinition = "text")
    private String status = "active";

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(nullable = false, columnDefinition = "text")
    private String currency = "INR";

    protected Loan() {
    }

    public Loan(
            UUID userId,
            String loanName,
            BigDecimal principalAmount,
            BigDecimal currentBalance,
            LocalDate startDate
    ) {
        this.userId = userId;
        this.loanName = loanName;
        this.principalAmount = principalAmount;
        this.currentBalance = currentBalance;
        this.startDate = startDate;
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
    public String getLoanName() { return loanName; }
    public BigDecimal getPrincipalAmount() { return principalAmount; }
    public BigDecimal getCurrentBalance() { return currentBalance; }
    public BigDecimal getInterestRate() { return interestRate; }
    public BigDecimal getMonthlyPayment() { return monthlyPayment; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public String getLender() { return lender; }
    public String getLoanType() { return loanType; }
    public String getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public String getCurrency() { return currency; }
}