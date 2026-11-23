package com.fintracker.coreapi.financial.dto.response;

import com.fintracker.coreapi.financial.entity.LoanPayment;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Public response projection for a loan payment. It exposes the payment details and parent loan ID without serializing the persistence entity or its internal owner identifier.
 */
public record LoanPaymentView(
        UUID id,
        UUID loanId,
        BigDecimal amount,
        BigDecimal principalAmount,
        BigDecimal interestAmount,
        LocalDate paymentDate,
        String paymentMethod,
        String notes,
        String currency,
        BigDecimal exchangeRate,
        BigDecimal inrEquivalent) {
    /**
     * Maps the persisted entity to the public response contract, explicitly selecting fields and excluding internal owner metadata.
     */
    public static LoanPaymentView from(LoanPayment e) {

        return new LoanPaymentView(
                e.getId(),
                e.getLoanId(),
                e.getAmount(),
                e.getPrincipalAmount(),
                e.getInterestAmount(),
                e.getPaymentDate(),
                e.getPaymentMethod(),
                e.getNotes(),
                e.getCurrency(),
                e.getExchangeRate(),
                e.getInrEquivalent());
    }
}
