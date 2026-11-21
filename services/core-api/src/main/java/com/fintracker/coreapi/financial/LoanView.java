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
 * Public response projection for a loan. It defines the response fields explicitly and does not expose the JPA entity or internal owner UUID.
 */
public record LoanView(
        UUID id,
        String loanName,
        BigDecimal principalAmount,
        BigDecimal currentBalance,
        BigDecimal interestRate,
        BigDecimal monthlyPayment,
        LocalDate startDate,
        LocalDate endDate,
        String lender,
        String loanType,
        String status,
        String currency) {
    /**
     * Maps the persisted entity to the public response contract, explicitly selecting fields and excluding internal owner metadata.
     */
    static LoanView from(Loan e) {

        return new LoanView(
                e.getId(),
                e.getLoanName(),
                e.getPrincipalAmount(),
                e.getCurrentBalance(),
                e.getInterestRate(),
                e.getMonthlyPayment(),
                e.getStartDate(),
                e.getEndDate(),
                e.getLender(),
                e.getLoanType(),
                e.getStatus(),
                e.getCurrency());
    }
}
