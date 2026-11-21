/**
 * Stable response projection for a loan.
 * Mapping the entity into this record makes the API's public field set explicit and avoids coupling JSON serialization to JPA annotations or internal owner metadata.
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
