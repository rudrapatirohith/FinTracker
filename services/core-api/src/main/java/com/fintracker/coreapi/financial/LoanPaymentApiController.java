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

@RestController @RequestMapping("/api/v1/loans/{loanId}/payments")
public class LoanPaymentApiController {
 private final LoanPaymentApiService service; LoanPaymentApiController(LoanPaymentApiService s){service=s;}
 @PostMapping ResponseEntity<LoanPaymentView> create(@PathVariable UUID loanId,@Valid @RequestBody LoanPaymentRequest r){return ResponseEntity.status(201).body(service.create(loanId,r));}
 @GetMapping Page<LoanPaymentView> all(@PathVariable UUID loanId,@PageableDefault(size=20,sort="paymentDate",direction=Sort.Direction.DESC) Pageable p){return service.all(loanId,p);}
 @GetMapping("/{paymentId}") LoanPaymentView one(@PathVariable UUID loanId,@PathVariable UUID paymentId){return service.one(loanId,paymentId);}
 @PutMapping("/{paymentId}") LoanPaymentView update(@PathVariable UUID loanId,@PathVariable UUID paymentId,@Valid @RequestBody LoanPaymentRequest r){return service.update(loanId,paymentId,r);}
 @DeleteMapping("/{paymentId}") ResponseEntity<Void> delete(@PathVariable UUID loanId,@PathVariable UUID paymentId){service.delete(loanId,paymentId);return ResponseEntity.noContent().build();}
}
