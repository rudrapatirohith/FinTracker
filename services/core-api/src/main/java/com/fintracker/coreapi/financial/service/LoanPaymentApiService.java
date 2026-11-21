package com.fintracker.coreapi.financial.service;

import com.fintracker.coreapi.financial.dto.request.LoanPaymentRequest;
import com.fintracker.coreapi.financial.dto.response.LoanPaymentView;
import com.fintracker.coreapi.financial.entity.LoanPayment;
import com.fintracker.coreapi.financial.repository.LoanPaymentRepository;
import com.fintracker.coreapi.identity.service.AuthenticatedUserProvider;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Application service for loan-payment use cases. It verifies the authenticated user's ownership of the parent loan and the payment, and intentionally leaves loan-balance accounting unchanged.
 */
@Service
public class LoanPaymentApiService {
    private final LoanPaymentRepository payments;
    private final LoanApiService loans;
    private final AuthenticatedUserProvider users;

    public LoanPaymentApiService(LoanPaymentRepository p, LoanApiService l, AuthenticatedUserProvider u) {
        payments = p;
        loans = l;
        users = u;
    }

    /**
     * Creates a payment only after confirming that the authenticated user owns the parent loan. The owner UUID comes from the security context, and creating a payment does not recalculate the loan balance.
     */
    @Transactional
    public LoanPaymentView create(UUID loanId, LoanPaymentRequest r) {
        UUID uid = users.getRequiredUserId();
        loans.requireOwned(loanId, uid);
        LoanPayment e = new LoanPayment(loanId, uid, r.amount(), r.paymentDate());

        apply(e, r);
        return LoanPaymentView.from(payments.save(e));
    }

    /**
     * First verifies ownership of the parent loan, then returns only payments matching both that loan ID and the authenticated user's UUID.
     */
    @Transactional(readOnly = true)
    public Page<LoanPaymentView> all(UUID loanId, Pageable p) {
        UUID uid = users.getRequiredUserId();
        loans.requireOwned(loanId, uid);
        return payments.findAllByLoanIdAndUserId(loanId, uid, p).map(LoanPaymentView::from);
    }

    /**
     * Returns a payment only when the caller owns the parent loan and the payment belongs to that same user and loan; all other cases return not found.
     */
    @Transactional(readOnly = true)
    public LoanPaymentView one(UUID loanId, UUID id) {
        return LoanPaymentView.from(owned(loanId, id));
    }

    /**
     * Verifies both parent-loan and payment ownership, then replaces payment details without changing the payment's owner or parent loan.
     */
    @Transactional
    public LoanPaymentView update(UUID loanId, UUID id, LoanPaymentRequest r) {
        LoanPayment e = owned(loanId, id);

        apply(e, r);
        return LoanPaymentView.from(e);
    }

    /**
     * Deletes the payment only after verifying that the authenticated user owns the parent loan and the payment belongs to that loan.
     */
    @Transactional
    public void delete(UUID loanId, UUID id) {
        payments.delete(owned(loanId, id));
    }

    /**
     * Checks parent-loan ownership first, then checks payment ownership and confirms the payment's loanId matches the route's loanId. A mismatch is reported as not found to avoid disclosing cross-user or cross-loan records.
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
     * Copies the amount, principal/interest split, date, payment method, notes, currency, and conversion values into the payment entity. Loan-balance accounting is intentionally outside this operation.
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
