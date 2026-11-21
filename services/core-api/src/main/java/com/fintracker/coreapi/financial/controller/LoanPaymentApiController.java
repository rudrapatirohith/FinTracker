package com.fintracker.coreapi.financial.controller;

import com.fintracker.coreapi.financial.dto.request.LoanPaymentRequest;
import com.fintracker.coreapi.financial.dto.response.LoanPaymentView;
import com.fintracker.coreapi.financial.service.LoanPaymentApiService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * HTTP adapter for the nested loan-payment resource. The loan ID comes from the route; the service verifies both parent-loan and payment ownership before returning or changing data.
 */
@RestController
@RequestMapping("/api/v1/loans/{loanId}/payments")
public class LoanPaymentApiController {
    private final LoanPaymentApiService service;

    public LoanPaymentApiController(LoanPaymentApiService s) {
        service = s;
    }

    /**
     * Creates a record for the authenticated user, applies the request's normalization rules, persists it, and returns the response projection.
     */
    @PostMapping
    public ResponseEntity<LoanPaymentView> create(@PathVariable UUID loanId, @Valid @RequestBody LoanPaymentRequest r) {
        return ResponseEntity.status(201).body(service.create(loanId, r));
    }

    /**
     * Returns a paginated collection belonging only to the authenticated user; the Pageable carries the requested page and sort.
     */
    @GetMapping
    public Page<LoanPaymentView> all(
            @PathVariable UUID loanId,
            @PageableDefault(size = 20, sort = "paymentDate", direction = Sort.Direction.DESC) Pageable p) {
        return service.all(loanId, p);
    }

    /**
     * Loads the requested record through an owner-scoped lookup; an absent ID and another user's ID produce the same not-found result.
     */
    @GetMapping("/{paymentId}")
    public LoanPaymentView one(@PathVariable UUID loanId, @PathVariable UUID paymentId) {
        return service.one(loanId, paymentId);
    }

    /**
     * Performs a full replacement of the payment's editable fields, not a partial merge. The request must describe the
     * complete intended payment state; optional values follow the service's replacement behavior. The service also
     * verifies ownership of the route's parent loan and payment, preserving both the payment ID and parent relationship.
     */
    @PutMapping("/{paymentId}")
    public LoanPaymentView update(
            @PathVariable UUID loanId, @PathVariable UUID paymentId, @Valid @RequestBody LoanPaymentRequest r) {
        return service.update(loanId, paymentId, r);
    }

    /**
     * Deletes the record only after an owner-scoped lookup, preventing a caller from deleting another user's data.
     */
    @DeleteMapping("/{paymentId}")
    public ResponseEntity<Void> delete(@PathVariable UUID loanId, @PathVariable UUID paymentId) {
        service.delete(loanId, paymentId);
        return ResponseEntity.noContent().build();
    }
}
