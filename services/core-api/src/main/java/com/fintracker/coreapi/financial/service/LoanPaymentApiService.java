package com.fintracker.coreapi.financial.service;

import com.fintracker.coreapi.financial.dto.request.LoanPaymentRequest;
import com.fintracker.coreapi.financial.dto.response.LoanPaymentView;
import com.fintracker.coreapi.financial.entity.Loan;
import com.fintracker.coreapi.financial.entity.LoanPayment;
import com.fintracker.coreapi.financial.repository.LoanPaymentRepository;
import com.fintracker.coreapi.identity.service.AuthenticatedUserProvider;
import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Application service for loan-payment use cases. It verifies the authenticated user's ownership of the parent loan and the payment, and updates the loan's outstanding principal balance atomically with each payment mutation.
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
     * Creates a payment only after confirming that the authenticated user owns the parent loan. The owner UUID comes from the security context, and the principal portion reduces the loan balance in the same database transaction.
     */
    @Transactional
    public LoanPaymentView create(UUID loanId, LoanPaymentRequest r) {
        UUID uid = users.getRequiredUserId();
        Loan parentLoan = loans.requireOwnedForUpdate(loanId, uid);
        validatePayment(r, parentLoan);
        if (r.principalAmount().compareTo(parentLoan.getCurrentBalance()) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Principal payment cannot exceed the current loan balance.");
        }
        LoanPayment e = new LoanPayment(loanId, uid, r.amount(), r.paymentDate());

        apply(e, r, parentLoan.getCurrency());
        parentLoan.adjustPrincipalBalance(r.principalAmount());
        return LoanPaymentView.from(payments.save(e));
    }

    /**
     * First verifies ownership of the parent loan, then returns only payments matching both that loan ID and the authenticated user's UUID.
     */
    @Transactional(readOnly = true)
    public Page<LoanPaymentView> all(UUID loanId, Pageable p) {
        UUID uid = users.getRequiredUserId();
        loans.requireOwned(loanId, uid);
        FinancialPageableValidator.validate(p, Set.of("paymentDate", "amount", "principalAmount", "createdAt"));
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
     * Verifies both parent-loan and payment ownership, then replaces payment details and applies only the principal difference to the parent loan balance.
     */
    @Transactional
    public LoanPaymentView update(UUID loanId, UUID id, LoanPaymentRequest r) {
        UUID uid = users.getRequiredUserId();
        Loan parentLoan = loans.requireOwnedForUpdate(loanId, uid);
        LoanPayment e = findOwnedPayment(loanId, id, uid);
        validatePayment(r, parentLoan);

        BigDecimal oldPrincipal = zeroIfNull(e.getPrincipalAmount());
        BigDecimal principalDelta = r.principalAmount().subtract(oldPrincipal);
        if (principalDelta.compareTo(parentLoan.getCurrentBalance()) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Principal adjustment cannot exceed the current loan balance.");
        }

        apply(e, r, parentLoan.getCurrency());
        parentLoan.adjustPrincipalBalance(principalDelta);
        return LoanPaymentView.from(e);
    }

    /**
     * Deletes the payment only after verifying ownership, then reverses its principal reduction in the same transaction.
     */
    @Transactional
    public void delete(UUID loanId, UUID id) {
        UUID uid = users.getRequiredUserId();
        Loan parentLoan = loans.requireOwnedForUpdate(loanId, uid);
        LoanPayment payment = findOwnedPayment(loanId, id, uid);
        BigDecimal principal = zeroIfNull(payment.getPrincipalAmount());
        payments.delete(payment);
        parentLoan.adjustPrincipalBalance(principal.negate());
    }

    /**
     * Checks parent-loan ownership first, then checks payment ownership and confirms the payment's loanId matches the route's loanId. A mismatch is reported as not found to avoid disclosing cross-user or cross-loan records.
     */
    private LoanPayment owned(UUID loanId, UUID id) {
        UUID uid = users.getRequiredUserId();
        loans.requireOwned(loanId, uid);
        return findOwnedPayment(loanId, id, uid);
    }

    /**
     * Loads a payment only when its ID, owner, and route parent loan all match.
     */
    private LoanPayment findOwnedPayment(UUID loanId, UUID id, UUID uid) {
        LoanPayment e = payments.findByIdAndUserId(id, uid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Loan payment not found."));

        if (!e.getLoanId().equals(loanId))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Loan payment not found.");
        return e;
    }

    /**
     * Validates the payment split and prevents applying principal in a currency different from the loan's balance currency.
     */
    private void validatePayment(LoanPaymentRequest request, Loan loan) {
        if (request.amount() == null
                || request.principalAmount() == null
                || request.interestAmount() == null
                || request.amount().compareTo(request.principalAmount().add(request.interestAmount())) != 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment amount must equal principal plus interest.");
        }
        String paymentCurrency = request.currency() == null ? loan.getCurrency() : request.currency();
        if (!loan.getCurrency().equalsIgnoreCase(paymentCurrency)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Loan payment currency must match the loan currency for balance accounting.");
        }
    }

    private BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    /**
     * Copies the amount, principal/interest split, date, payment method, notes, currency, and conversion values into the payment entity. If currency is omitted, it inherits the parent loan's currency. The caller adjusts the parent loan balance in the same transaction using the principal delta.
     */
    private void apply(LoanPayment e, LoanPaymentRequest r, String loanCurrency) {
        e.updateDetails(
                r.amount(),
                r.principalAmount(),
                r.interestAmount(),
                r.paymentDate(),
                r.paymentMethod(),
                r.notes(),
                r.currency() == null ? loanCurrency : r.currency(),
                r.exchangeRate(),
                r.inrEquivalent());
    }
}
