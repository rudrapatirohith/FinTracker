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

/**
 * HTTP adapter for loan endpoints. It validates request DTOs, defines pagination defaults and HTTP status codes, and delegates ownership and persistence decisions to LoanApiService.
 */
@RestController
@RequestMapping("/api/v1/loans")
public class LoanApiController {
    private final LoanApiService service;

    LoanApiController(LoanApiService s) {
        service = s;
    }

    /**
     * Creates a record for the authenticated user, applies the request's normalization rules, persists it, and returns the response projection.
     */
    @PostMapping
    ResponseEntity<LoanView> create(@Valid @RequestBody LoanRequest r) {
        return ResponseEntity.status(201).body(service.create(r));
    }

    /**
     * Returns a paginated collection belonging only to the authenticated user; the Pageable carries the requested page and sort.
     */
    @GetMapping
    Page<LoanView> all(@PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable p) {
        return service.all(p);
    }

    /**
     * Loads the requested record through an owner-scoped lookup; an absent ID and another user's ID produce the same not-found result.
     */
    @GetMapping("/{id}")
    LoanView one(@PathVariable UUID id) {
        return service.one(id);
    }

    /**
     * Replaces the record's editable business fields after an owner-scoped lookup, while preserving its ID and owner.
     */
    @PutMapping("/{id}")
    LoanView update(@PathVariable UUID id, @Valid @RequestBody LoanRequest r) {
        return service.update(id, r);
    }

    /**
     * Deletes the record only after an owner-scoped lookup, preventing a caller from deleting another user's data.
     */
    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
