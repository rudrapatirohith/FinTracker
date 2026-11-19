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
import jakarta.persistence.Table;

@Entity
@Table(name = "loan_payments")
public class LoanPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "loan_id", nullable = false)
    private UUID loanId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "principal_amount", precision = 12, scale = 2)
    private BigDecimal principalAmount;

    @Column(name = "interest_amount", precision = 12, scale = 2)
    private BigDecimal interestAmount;

    @Column(name = "payment_date", nullable = false)
    private LocalDate paymentDate;

    @Column(name = "payment_method", columnDefinition = "text")
    private String paymentMethod;

    @Column(columnDefinition = "text")
    private String notes;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(nullable = false, columnDefinition = "text")
    private String currency = "INR";

    @Column(name = "exchange_rate", precision = 10, scale = 6)
    private BigDecimal exchangeRate;

    @Column(name = "inr_equivalent", precision = 15, scale = 2)
    private BigDecimal inrEquivalent;

    protected LoanPayment() {
    }

    public LoanPayment(
            UUID loanId,
            UUID userId,
            BigDecimal amount,
            LocalDate paymentDate
    ) {
        this.loanId = loanId;
        this.userId = userId;
        this.amount = amount;
        this.paymentDate = paymentDate;
    }

    @jakarta.persistence.PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    /** Updates payment data; loan and owner linkage remain immutable. */
    public void updateDetails(BigDecimal amount,BigDecimal principal,BigDecimal interest,LocalDate date,String method,String notes,String currency,BigDecimal rate,BigDecimal inr) {
        this.amount=amount; principalAmount=principal; interestAmount=interest; paymentDate=date;
        paymentMethod=method; this.notes=notes; this.currency=currency; exchangeRate=rate; inrEquivalent=inr;
    }

    public UUID getId() { return id; }
    public UUID getLoanId() { return loanId; }
    public UUID getUserId() { return userId; }
    public BigDecimal getAmount() { return amount; }
    public BigDecimal getPrincipalAmount() { return principalAmount; }
    public BigDecimal getInterestAmount() { return interestAmount; }
    public LocalDate getPaymentDate() { return paymentDate; }
    public String getPaymentMethod() { return paymentMethod; }
    public String getNotes() { return notes; }
    public Instant getCreatedAt() { return createdAt; }
    public String getCurrency() { return currency; }
    public BigDecimal getExchangeRate() { return exchangeRate; }
    public BigDecimal getInrEquivalent() { return inrEquivalent; }
}