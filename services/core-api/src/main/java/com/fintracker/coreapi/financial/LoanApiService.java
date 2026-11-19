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

@Service public class LoanApiService {
 private final LoanRepository repo; private final AuthenticatedUserProvider users;
 LoanApiService(LoanRepository r,AuthenticatedUserProvider u){repo=r;users=u;}
 @Transactional LoanView create(LoanRequest r){Loan e=new Loan(users.getRequiredUserId(),r.loanName().trim(),r.principalAmount(),r.currentBalance(),r.startDate());apply(e,r);return LoanView.from(repo.save(e));}
 @Transactional(readOnly=true) Page<LoanView> all(Pageable p){return repo.findAllByUserId(users.getRequiredUserId(),p).map(LoanView::from);}
 @Transactional(readOnly=true) LoanView one(UUID id){return LoanView.from(owned(id));}
 @Transactional LoanView update(UUID id,LoanRequest r){Loan e=owned(id);apply(e,r);return LoanView.from(e);}
 @Transactional void delete(UUID id){repo.delete(owned(id));}
 Loan requireOwned(UUID id,UUID uid){return repo.findByIdAndUserId(id,uid).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Loan not found."));}
 private Loan owned(UUID id){return requireOwned(id,users.getRequiredUserId());}
 private void apply(Loan e,LoanRequest r){if(r.endDate()!=null&&r.endDate().isBefore(r.startDate()))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"endDate must be on or after startDate.");e.updateDetails(r.loanName().trim(),r.principalAmount(),r.currentBalance(),r.interestRate(),r.monthlyPayment(),r.startDate(),r.endDate(),r.lender(),r.loanType(),r.status(),r.currency().toUpperCase(Locale.ROOT));}
}
