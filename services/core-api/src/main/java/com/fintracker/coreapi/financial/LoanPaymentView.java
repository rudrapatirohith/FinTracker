/**
 * Read-only API projection for a loan payment.
 * The mapper explicitly selects fields exposed to clients instead of serializing the JPA entity, keeping persistence details and tenant ownership metadata out of the response.
 */
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
