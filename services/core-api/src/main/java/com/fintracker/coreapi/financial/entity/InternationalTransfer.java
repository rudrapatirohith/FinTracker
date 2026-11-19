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
@Table(name = "international_transfers")
public class InternationalTransfer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "recipient_name", nullable = false, columnDefinition = "text")
    private String recipientName;

    @Column(name = "recipient_country", nullable = false, columnDefinition = "text")
    private String recipientCountry;

    @Column(name = "amount_sent", nullable = false, precision = 12, scale = 2)
    private BigDecimal amountSent;

    @Column(name = "currency_sent", columnDefinition = "text")
    private String currencySent = "USD";

    @Column(name = "amount_received", nullable = false, precision = 12, scale = 2)
    private BigDecimal amountReceived;

    @Column(name = "currency_received", columnDefinition = "text")
    private String currencyReceived = "INR";

    @Column(name = "exchange_rate", nullable = false, precision = 10, scale = 6)
    private BigDecimal exchangeRate;

    @Column(name = "transfer_fee", precision = 12, scale = 2)
    private BigDecimal transferFee = BigDecimal.ZERO;

    @Column(name = "transfer_method", columnDefinition = "text")
    private String transferMethod;

    @Column(columnDefinition = "text")
    private String purpose;

    @Column(columnDefinition = "text")
    private String status = "pending";

    @Column(name = "transfer_date", nullable = false)
    private LocalDate transferDate;

    @Column(name = "completion_date")
    private LocalDate completionDate;

    @Column(name = "reference_number", columnDefinition = "text")
    private String referenceNumber;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "sent_currency", columnDefinition = "text")
    private String sentCurrency = "USD";

    @Column(name = "received_currency", columnDefinition = "text")
    private String receivedCurrency = "INR";

    @Column(name = "usd_to_inr_rate", precision = 10, scale = 6)
    private BigDecimal usdToInrRate;

    @Column(name = "inr_equivalent", precision = 15, scale = 2)
    private BigDecimal inrEquivalent;

    protected InternationalTransfer() {
    }

    public InternationalTransfer(
            UUID userId,
            String recipientName,
            String recipientCountry,
            BigDecimal amountSent,
            BigDecimal amountReceived,
            BigDecimal exchangeRate,
            LocalDate transferDate
    ) {
        this.userId = userId;
        this.recipientName = recipientName;
        this.recipientCountry = recipientCountry;
        this.amountSent = amountSent;
        this.amountReceived = amountReceived;
        this.exchangeRate = exchangeRate;
        this.transferDate = transferDate;
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
    public String getRecipientName() { return recipientName; }
    public String getRecipientCountry() { return recipientCountry; }
    public BigDecimal getAmountSent() { return amountSent; }
    public String getCurrencySent() { return currencySent; }
    public BigDecimal getAmountReceived() { return amountReceived; }
    public String getCurrencyReceived() { return currencyReceived; }
    public BigDecimal getExchangeRate() { return exchangeRate; }
    public BigDecimal getTransferFee() { return transferFee; }
    public String getTransferMethod() { return transferMethod; }
    public String getPurpose() { return purpose; }
    public String getStatus() { return status; }
    public LocalDate getTransferDate() { return transferDate; }
    public LocalDate getCompletionDate() { return completionDate; }
    public String getReferenceNumber() { return referenceNumber; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public String getSentCurrency() { return sentCurrency; }
    public String getReceivedCurrency() { return receivedCurrency; }
    public BigDecimal getUsdToInrRate() { return usdToInrRate; }
    public BigDecimal getInrEquivalent() { return inrEquivalent; }
}
