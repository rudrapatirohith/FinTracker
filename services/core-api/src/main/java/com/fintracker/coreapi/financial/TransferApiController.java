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
 * HTTP adapter for international-transfer endpoints. It validates request bodies and defines HTTP/pagination behavior while delegating tenant checks and persistence to TransferApiService.
 */
@RestController
@RequestMapping("/api/v1/international-transfers")
public class TransferApiController {
    private final TransferApiService service;

    TransferApiController(TransferApiService s) {
        service = s;
    }

    /**
     * Creates a record for the authenticated user, applies the request's normalization rules, persists it, and returns the response projection.
     */
    @PostMapping
    ResponseEntity<TransferView> create(@Valid @RequestBody TransferRequest r) {
        return ResponseEntity.status(201).body(service.create(r));
    }

    /**
     * Returns a paginated collection belonging only to the authenticated user; the Pageable carries the requested page and sort.
     */
    @GetMapping
    Page<TransferView> all(
            @PageableDefault(size = 20, sort = "transferDate", direction = Sort.Direction.DESC) Pageable p) {
        return service.all(p);
    }

    /**
     * Loads the requested record through an owner-scoped lookup; an absent ID and another user's ID produce the same not-found result.
     */
    @GetMapping("/{id}")
    TransferView one(@PathVariable UUID id) {
        return service.one(id);
    }

    /**
     * Replaces the record's editable business fields after an owner-scoped lookup, while preserving its ID and owner.
     */
    @PutMapping("/{id}")
    TransferView update(@PathVariable UUID id, @Valid @RequestBody TransferRequest r) {
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
