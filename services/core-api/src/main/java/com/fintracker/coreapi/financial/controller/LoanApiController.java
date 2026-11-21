package com.fintracker.coreapi.financial.controller;

import com.fintracker.coreapi.financial.dto.request.LoanRequest;
import com.fintracker.coreapi.financial.dto.response.LoanView;
import com.fintracker.coreapi.financial.service.LoanApiService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * HTTP adapter for loan endpoints. It validates request DTOs, defines pagination defaults and HTTP status codes, and delegates ownership and persistence decisions to LoanApiService.
 */
@RestController
@RequestMapping("/api/v1/loans")
public class LoanApiController {
    private final LoanApiService service;

    public LoanApiController(LoanApiService s) {
        service = s;
    }

    /**
     * Creates a record for the authenticated user, applies the request's normalization rules, persists it, and returns the response projection.
     */
    @PostMapping
    public ResponseEntity<LoanView> create(@Valid @RequestBody LoanRequest r) {
        return ResponseEntity.status(201).body(service.create(r));
    }

    /**
     * Returns a paginated collection belonging only to the authenticated user; the Pageable carries the requested page and sort.
     */
    @GetMapping
    public Page<LoanView> all(@PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable p) {
        return service.all(p);
    }

    /**
     * Loads the requested record through an owner-scoped lookup; an absent ID and another user's ID produce the same not-found result.
     */
    @GetMapping("/{id}")
    public LoanView one(@PathVariable UUID id) {
        return service.one(id);
    }

    /**
     * Performs a full replacement of the loan's editable fields. The request must describe the complete intended loan state;
     * optional values are applied using the service's replacement/default rules rather than merged with stored values.
     * The owner-scoped service lookup preserves the existing loan ID and owner.
     */
    @PutMapping("/{id}")
    public LoanView update(@PathVariable UUID id, @Valid @RequestBody LoanRequest r) {
        return service.update(id, r);
    }

    /**
     * Deletes the record only after an owner-scoped lookup, preventing a caller from deleting another user's data.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
