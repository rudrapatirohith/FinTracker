package com.fintracker.coreapi.financial;

import com.fintracker.coreapi.financial.entity.*;
import com.fintracker.coreapi.financial.repository.*;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

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
    static LoanPaymentView from(LoanPayment e) {

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
