package com.fintracker.coreapi.financial;

import com.fintracker.coreapi.financial.entity.*;
import com.fintracker.coreapi.financial.repository.*;
import com.fintracker.coreapi.identity.service.AuthenticatedUserProvider;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.util.UUID;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@Service
public class LoanPaymentApiService {
    private final LoanPaymentRepository payments;
    private final LoanApiService loans;
    private final AuthenticatedUserProvider users;

  
    LoanPaymentApiService(LoanPaymentRepository p, LoanApiService l, AuthenticatedUserProvider u) {
        payments = p;
        loans = l;
        users = u;
    }

    /**
     * Creates a record for the authenticated user, applies the request's normalization rules, persists it, and returns the response projection.
     */
    @Transactional
    LoanPaymentView create(UUID loanId, LoanPaymentRequest r) {
        UUID uid = users.getRequiredUserId();
        loans.requireOwned(loanId, uid);
        LoanPayment e = new LoanPayment(loanId, uid, r.amount(), r.paymentDate());
      
        apply(e, r);
        return LoanPaymentView.from(payments.save(e));
    }

    /**
     * Returns a paginated collection belonging only to the authenticated user; the Pageable carries the requested page and sort.
     */
    @Transactional(readOnly = true)
    Page<LoanPaymentView> all(UUID loanId, Pageable p) {
        UUID uid = users.getRequiredUserId();
        loans.requireOwned(loanId, uid);
        return payments.findAllByLoanIdAndUserId(loanId, uid, p).map(LoanPaymentView::from);
    }

    /**
     * Loads the requested record through an owner-scoped lookup; an absent ID and another user's ID produce the same not-found result.
     */
    @Transactional(readOnly = true)
    LoanPaymentView one(UUID loanId, UUID id) {
        return LoanPaymentView.from(owned(loanId, id));
    }

    /**
     * Replaces the record's editable business fields after an owner-scoped lookup, while preserving its ID and owner.
     */
    @Transactional
    LoanPaymentView update(UUID loanId, UUID id, LoanPaymentRequest r) {
        LoanPayment e = owned(loanId, id);
      
        apply(e, r);
        return LoanPaymentView.from(e);
    }

    /**
     * Deletes the record only after an owner-scoped lookup, preventing a caller from deleting another user's data.
     */
    @Transactional
    void delete(UUID loanId, UUID id) {
        payments.delete(owned(loanId, id));
    }

    /**
     * Resolves the authenticated user's ID and delegates to the owner-scoped repository lookup.
     */
    private LoanPayment owned(UUID loanId, UUID id) {
        UUID uid = users.getRequiredUserId();
        loans.requireOwned(loanId, uid);
        LoanPayment e = payments.findByIdAndUserId(id, uid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Loan payment not found."));
      
        if (!e.getLoanId().equals(loanId))
            
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Loan payment not found.");
        return e;
    }

    /**
     * Copies request values into the entity and centralizes trimming, currency normalization, defaults, and cross-field validation so create and update follow identical rules.
     */
    private void apply(LoanPayment e, LoanPaymentRequest r) {
        e.updateDetails(
                r.amount(),
                r.principalAmount(),
                r.interestAmount(),
                r.paymentDate(),
                r.paymentMethod(),
                r.notes(),
                r.currency(),
                r.exchangeRate(),
                r.inrEquivalent());
    }
}
