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

/*
 * Financial transfer and loan API implementation. These package-private
 * request/response records and Spring components intentionally share one
 * source unit to keep this additive change atomic.
 */
record TransferRequest(@NotBlank @Size(max=255) String recipientName,@NotBlank @Size(max=255) String recipientCountry,
 @NotNull @DecimalMin("0.01") @Digits(integer=10,fraction=2) BigDecimal amountSent,@NotBlank @Pattern(regexp="INR|USD|EUR|GBP|CAD|AUD") String currencySent,
 @NotNull @DecimalMin("0.01") @Digits(integer=10,fraction=2) BigDecimal amountReceived,@NotBlank @Pattern(regexp="INR|USD|EUR|GBP|CAD|AUD") String currencyReceived,
 @NotNull @DecimalMin("0.000001") @Digits(integer=4,fraction=6) BigDecimal exchangeRate,@DecimalMin("0.00") @Digits(integer=10,fraction=2) BigDecimal transferFee,
 @Size(max=100) String transferMethod,@Size(max=2000) String purpose,@NotBlank @Size(max=50) String status,@NotNull LocalDate transferDate,LocalDate completionDate,
 @Size(max=255) String referenceNumber,@DecimalMin("0.000001") @Digits(integer=4,fraction=6) BigDecimal usdToInrRate,@DecimalMin("0.00") @Digits(integer=13,fraction=2) BigDecimal inrEquivalent) {}
record TransferView(UUID id,String recipientName,String recipientCountry,BigDecimal amountSent,String currencySent,BigDecimal amountReceived,String currencyReceived,BigDecimal exchangeRate,BigDecimal transferFee,String transferMethod,String purpose,String status,LocalDate transferDate,LocalDate completionDate,String referenceNumber,BigDecimal usdToInrRate,BigDecimal inrEquivalent) {
 static TransferView from(InternationalTransfer e){return new TransferView(e.getId(),e.getRecipientName(),e.getRecipientCountry(),e.getAmountSent(),e.getCurrencySent(),e.getAmountReceived(),e.getCurrencyReceived(),e.getExchangeRate(),e.getTransferFee(),e.getTransferMethod(),e.getPurpose(),e.getStatus(),e.getTransferDate(),e.getCompletionDate(),e.getReferenceNumber(),e.getUsdToInrRate(),e.getInrEquivalent());}
}
@Service class TransferApiService {
 private final InternationalTransferRepository repo; private final AuthenticatedUserProvider users;
 TransferApiService(InternationalTransferRepository r,AuthenticatedUserProvider u){repo=r;users=u;}
 @Transactional TransferView create(TransferRequest r){UUID uid=users.getRequiredUserId();InternationalTransfer e=new InternationalTransfer(uid,r.recipientName().trim(),r.recipientCountry().trim(),r.amountSent(),r.amountReceived(),r.exchangeRate(),r.transferDate());apply(e,r);return TransferView.from(repo.save(e));}
 @Transactional(readOnly=true) Page<TransferView> all(Pageable p){return repo.findAllByUserId(users.getRequiredUserId(),p).map(TransferView::from);}
 @Transactional(readOnly=true) TransferView one(UUID id){return TransferView.from(owned(id));}
 @Transactional TransferView update(UUID id,TransferRequest r){InternationalTransfer e=owned(id);apply(e,r);return TransferView.from(e);}
 @Transactional void delete(UUID id){repo.delete(owned(id));}
 private InternationalTransfer owned(UUID id){return repo.findByIdAndUserId(id,users.getRequiredUserId()).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Transfer not found."));}
 private void apply(InternationalTransfer e,TransferRequest r){String a=r.currencySent().toUpperCase(Locale.ROOT),b=r.currencyReceived().toUpperCase(Locale.ROOT);e.updateDetails(r.recipientName().trim(),r.recipientCountry().trim(),r.amountSent(),a,r.amountReceived(),b,r.exchangeRate(),r.transferFee()==null?BigDecimal.ZERO:r.transferFee(),r.transferMethod(),r.purpose(),r.status().trim().toLowerCase(Locale.ROOT),r.transferDate(),r.completionDate(),r.referenceNumber(),r.usdToInrRate(),r.inrEquivalent());}
}
@RestController @RequestMapping("/api/v1/international-transfers")
class TransferApiController {
 private final TransferApiService service; TransferApiController(TransferApiService s){service=s;}
 @PostMapping ResponseEntity<TransferView> create(@Valid @RequestBody TransferRequest r){return ResponseEntity.status(201).body(service.create(r));}
 @GetMapping Page<TransferView> all(@PageableDefault(size=20,sort="transferDate",direction=Sort.Direction.DESC) Pageable p){return service.all(p);}
 @GetMapping("/{id}") TransferView one(@PathVariable UUID id){return service.one(id);}
 @PutMapping("/{id}") TransferView update(@PathVariable UUID id,@Valid @RequestBody TransferRequest r){return service.update(id,r);}
 @DeleteMapping("/{id}") ResponseEntity<Void> delete(@PathVariable UUID id){service.delete(id);return ResponseEntity.noContent().build();}
}

record LoanRequest(@NotBlank @Size(max=255) String loanName,@NotNull @DecimalMin("0.01") @Digits(integer=10,fraction=2) BigDecimal principalAmount,
 @NotNull @DecimalMin("0.00") @Digits(integer=10,fraction=2) BigDecimal currentBalance,@DecimalMin("0.00") @Digits(integer=3,fraction=2) BigDecimal interestRate,
 @DecimalMin("0.00") @Digits(integer=10,fraction=2) BigDecimal monthlyPayment,@NotNull LocalDate startDate,LocalDate endDate,@Size(max=255) String lender,
 @Size(max=100) String loanType,@NotBlank @Pattern(regexp="active|paid|closed|defaulted") String status,@NotBlank @Pattern(regexp="INR|USD") String currency) {}
record LoanView(UUID id,String loanName,BigDecimal principalAmount,BigDecimal currentBalance,BigDecimal interestRate,BigDecimal monthlyPayment,LocalDate startDate,LocalDate endDate,String lender,String loanType,String status,String currency) {
 static LoanView from(Loan e){return new LoanView(e.getId(),e.getLoanName(),e.getPrincipalAmount(),e.getCurrentBalance(),e.getInterestRate(),e.getMonthlyPayment(),e.getStartDate(),e.getEndDate(),e.getLender(),e.getLoanType(),e.getStatus(),e.getCurrency());}
}
@Service class LoanApiService {
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
@RestController @RequestMapping("/api/v1/loans")
class LoanApiController {
 private final LoanApiService service; LoanApiController(LoanApiService s){service=s;}
 @PostMapping ResponseEntity<LoanView> create(@Valid @RequestBody LoanRequest r){return ResponseEntity.status(201).body(service.create(r));}
 @GetMapping Page<LoanView> all(@PageableDefault(size=20,sort="createdAt",direction=Sort.Direction.DESC) Pageable p){return service.all(p);}
 @GetMapping("/{id}") LoanView one(@PathVariable UUID id){return service.one(id);}
 @PutMapping("/{id}") LoanView update(@PathVariable UUID id,@Valid @RequestBody LoanRequest r){return service.update(id,r);}
 @DeleteMapping("/{id}") ResponseEntity<Void> delete(@PathVariable UUID id){service.delete(id);return ResponseEntity.noContent().build();}
}

record LoanPaymentRequest(@NotNull @DecimalMin("0.01") @Digits(integer=10,fraction=2) BigDecimal amount,@DecimalMin("0.00") @Digits(integer=10,fraction=2) BigDecimal principalAmount,
 @DecimalMin("0.00") @Digits(integer=10,fraction=2) BigDecimal interestAmount,@NotNull LocalDate paymentDate,@Size(max=100) String paymentMethod,@Size(max=2000) String notes,
 @NotBlank @Pattern(regexp="INR|USD") String currency,@DecimalMin("0.000001") @Digits(integer=4,fraction=6) BigDecimal exchangeRate,@DecimalMin("0.00") @Digits(integer=13,fraction=2) BigDecimal inrEquivalent) {}
record LoanPaymentView(UUID id,UUID loanId,BigDecimal amount,BigDecimal principalAmount,BigDecimal interestAmount,LocalDate paymentDate,String paymentMethod,String notes,String currency,BigDecimal exchangeRate,BigDecimal inrEquivalent) {
 static LoanPaymentView from(LoanPayment e){return new LoanPaymentView(e.getId(),e.getLoanId(),e.getAmount(),e.getPrincipalAmount(),e.getInterestAmount(),e.getPaymentDate(),e.getPaymentMethod(),e.getNotes(),e.getCurrency(),e.getExchangeRate(),e.getInrEquivalent());}
}
@Service class LoanPaymentApiService {
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
@RestController @RequestMapping("/api/v1/loans/{loanId}/payments")
class LoanPaymentApiController {
 private final LoanPaymentApiService service; LoanPaymentApiController(LoanPaymentApiService s){service=s;}
 @PostMapping ResponseEntity<LoanPaymentView> create(@PathVariable UUID loanId,@Valid @RequestBody LoanPaymentRequest r){return ResponseEntity.status(201).body(service.create(loanId,r));}
 @GetMapping Page<LoanPaymentView> all(@PathVariable UUID loanId,@PageableDefault(size=20,sort="paymentDate",direction=Sort.Direction.DESC) Pageable p){return service.all(loanId,p);}
 @GetMapping("/{paymentId}") LoanPaymentView one(@PathVariable UUID loanId,@PathVariable UUID paymentId){return service.one(loanId,paymentId);}
 @PutMapping("/{paymentId}") LoanPaymentView update(@PathVariable UUID loanId,@PathVariable UUID paymentId,@Valid @RequestBody LoanPaymentRequest r){return service.update(loanId,paymentId,r);}
 @DeleteMapping("/{paymentId}") ResponseEntity<Void> delete(@PathVariable UUID loanId,@PathVariable UUID paymentId){service.delete(loanId,paymentId);return ResponseEntity.noContent().build();}
}
