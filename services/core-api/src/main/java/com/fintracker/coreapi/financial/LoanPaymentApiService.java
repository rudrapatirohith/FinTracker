/**
 * Transactional use cases for loan payments.
 * Each operation first verifies that the authenticated user owns the parent loan, then verifies that the payment belongs to that same user and requested loan.
 * This two-level check prevents a caller from reaching another user's payment by changing either path identifier.
 * Payment creation intentionally does not recalculate loan balance because principal/interest accounting rules are not yet defined.
 */
package com.fintracker.coreapi.financial;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Locale;
import java.util.UUID;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import com.fintracker.coreapi.financial.entity.*;
import com.fintracker.coreapi.financial.repository.*;
import com.fintracker.coreapi.identity.service.AuthenticatedUserProvider;

@Service public class LoanPaymentApiService {
 private final LoanPaymentRepository payments; private final LoanApiService loans; private final AuthenticatedUserProvider users;
 LoanPaymentApiService(LoanPaymentRepository p,LoanApiService l,AuthenticatedUserProvider u){payments=p;loans=l;users=u;}
 @Transactional LoanPaymentView create(UUID loanId,LoanPaymentRequest r){UUID uid=users.getRequiredUserId();loans.requireOwned(loanId,uid);LoanPayment e=new LoanPayment(loanId,uid,r.amount(),r.paymentDate());apply(e,r);return LoanPaymentView.from(payments.save(e));}
 @Transactional(readOnly=true) Page<LoanPaymentView> all(UUID loanId,Pageable p){UUID uid=users.getRequiredUserId();loans.requireOwned(loanId,uid);return payments.findAllByLoanIdAndUserId(loanId,uid,p).map(LoanPaymentView::from);}
 @Transactional(readOnly=true) LoanPaymentView one(UUID loanId,UUID id){return LoanPaymentView.from(owned(loanId,id));}
 @Transactional LoanPaymentView update(UUID loanId,UUID id,LoanPaymentRequest r){LoanPayment e=owned(loanId,id);apply(e,r);return LoanPaymentView.from(e);}
 @Transactional void delete(UUID loanId,UUID id){payments.delete(owned(loanId,id));}
 private LoanPayment owned(UUID loanId,UUID id){UUID uid=users.getRequiredUserId();loans.requireOwned(loanId,uid);LoanPayment e=payments.findByIdAndUserId(id,uid).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Loan payment not found."));if(!e.getLoanId().equals(loanId))throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Loan payment not found.");return e;}
 private void apply(LoanPayment e,LoanPaymentRequest r){e.updateDetails(r.amount(),r.principalAmount(),r.interestAmount(),r.paymentDate(),r.paymentMethod(),r.notes(),r.currency(),r.exchangeRate(),r.inrEquivalent());}
}
