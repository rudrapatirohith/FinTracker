package com.fintracker.coreapi.financial;

import com.fintracker.coreapi.financial.entity.*;
import com.fintracker.coreapi.financial.repository.*;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.UUID;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/loans/{loanId}/payments")
public class LoanPaymentApiController {
    private final LoanPaymentApiService service;

  /**
   * Receives the service dependency used by this HTTP adapter; constructor injection keeps the controller's application-service dependency explicit.
   */
    LoanPaymentApiController(LoanPaymentApiService s) {
        service = s;
    }

    /**
     * Creates a record for the authenticated user, applies the request's normalization rules, persists it, and returns the response projection.
     */
    @PostMapping
    ResponseEntity<LoanPaymentView> create(@PathVariable UUID loanId, @Valid @RequestBody LoanPaymentRequest r) {
        return ResponseEntity.status(201).body(service.create(loanId, r));
    }

    /**
     * Returns a paginated collection belonging only to the authenticated user; the Pageable carries the requested page and sort.
     */
    @GetMapping
    Page<LoanPaymentView> all(
            @PathVariable UUID loanId,
            @PageableDefault(size = 20, sort = "paymentDate", direction = Sort.Direction.DESC) Pageable p) {
        return service.all(loanId, p);
    }

    /**
     * Loads the requested record through an owner-scoped lookup; an absent ID and another user's ID produce the same not-found result.
     */
    @GetMapping("/{paymentId}")
    LoanPaymentView one(@PathVariable UUID loanId, @PathVariable UUID paymentId) {
        return service.one(loanId, paymentId);
    }

    /**
     * Replaces the record's editable business fields after an owner-scoped lookup, while preserving its ID and owner.
     */
    @PutMapping("/{paymentId}")
    LoanPaymentView update(
            @PathVariable UUID loanId, @PathVariable UUID paymentId, @Valid @RequestBody LoanPaymentRequest r) {
        return service.update(loanId, paymentId, r);
    }

    /**
     * Deletes the record only after an owner-scoped lookup, preventing a caller from deleting another user's data.
     */
    @DeleteMapping("/{paymentId}")
    ResponseEntity<Void> delete(@PathVariable UUID loanId, @PathVariable UUID paymentId) {
        service.delete(loanId, paymentId);
        return ResponseEntity.noContent().build();
    }
}
