package com.fintracker.coreapi.financial;

import com.fintracker.coreapi.financial.entity.*;
import com.fintracker.coreapi.financial.repository.*;
import com.fintracker.coreapi.identity.service.AuthenticatedUserProvider;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.util.Locale;
import java.util.UUID;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/**
 * Application service for loan use cases. It derives ownership from the authenticated JWT, scopes repository access by owner, validates date relationships, and maps entities to API views.
 */
@Service
public class LoanApiService {
    private final LoanRepository repo;
    private final AuthenticatedUserProvider users;

    LoanApiService(LoanRepository r, AuthenticatedUserProvider u) {
        repo = r;
        users = u;
    }

    /**
     * Creates a record for the authenticated user, applies the request's normalization rules, persists it, and returns the response projection.
     */
    @Transactional
    LoanView create(LoanRequest r) {
        Loan e = new Loan(
                users.getRequiredUserId(), r.loanName().trim(), r.principalAmount(), r.currentBalance(), r.startDate());

        apply(e, r);
        return LoanView.from(repo.save(e));
    }

    /**
     * Returns a paginated collection belonging only to the authenticated user; the Pageable carries the requested page and sort.
     */
    @Transactional(readOnly = true)
    Page<LoanView> all(Pageable p) {
        return repo.findAllByUserId(users.getRequiredUserId(), p).map(LoanView::from);
    }

    /**
     * Loads the requested record through an owner-scoped lookup; an absent ID and another user's ID produce the same not-found result.
     */
    @Transactional(readOnly = true)
    LoanView one(UUID id) {
        return LoanView.from(owned(id));
    }

    /**
     * Replaces the record's editable business fields after an owner-scoped lookup, while preserving its ID and owner.
     */
    @Transactional
    LoanView update(UUID id, LoanRequest r) {
        Loan e = owned(id);

        apply(e, r);
        return LoanView.from(e);
    }

    /**
     * Deletes the record only after an owner-scoped lookup, preventing a caller from deleting another user's data.
     */
    @Transactional
    void delete(UUID id) {
        repo.delete(owned(id));
    }

    /**
     * Finds a loan only when both the loan ID and supplied owner ID match; nested loan-payment operations use this before accessing payments.
     */
    Loan requireOwned(UUID id, UUID uid) {
        return repo.findByIdAndUserId(id, uid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Loan not found."));
    }

    /**
     * Resolves the authenticated user's ID and delegates to the owner-scoped repository lookup.
     */
    private Loan owned(UUID id) {

        return requireOwned(id, users.getRequiredUserId());
    }

    /**
     * Copies request values into the entity and centralizes trimming, currency normalization, defaults, and cross-field validation so create and update follow identical rules.
     */
    private void apply(Loan e, LoanRequest r) {

        if (r.endDate() != null && r.endDate().isBefore(r.startDate()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "endDate must be on or after startDate.");
        e.updateDetails(
                r.loanName().trim(),
                r.principalAmount(),
                r.currentBalance(),
                r.interestRate(),
                r.monthlyPayment(),
                r.startDate(),
                r.endDate(),
                r.lender(),
                r.loanType(),
                r.status(),
                r.currency().toUpperCase(Locale.ROOT));
    }
}
