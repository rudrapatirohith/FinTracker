package com.fintracker.coreapi.financial.controller;

import com.fintracker.coreapi.financial.dto.request.TransferRequest;
import com.fintracker.coreapi.financial.dto.response.TransferView;
import com.fintracker.coreapi.financial.service.TransferApiService;
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
 * HTTP adapter for international-transfer endpoints. It validates request bodies and defines HTTP/pagination behavior while delegating tenant checks and persistence to TransferApiService.
 */
@RestController
@RequestMapping("/api/v1/international-transfers")
public class TransferApiController {
    private final TransferApiService service;

    public TransferApiController(TransferApiService s) {
        service = s;
    }

    /**
     * Creates a record for the authenticated user, applies the request's normalization rules, persists it, and returns the response projection.
     */
    @PostMapping
    public ResponseEntity<TransferView> create(@Valid @RequestBody TransferRequest r) {
        return ResponseEntity.status(201).body(service.create(r));
    }

    /**
     * Returns a paginated collection belonging only to the authenticated user; the Pageable carries the requested page and sort.
     */
    @GetMapping
    public Page<TransferView> all(
            @PageableDefault(size = 20, sort = "transferDate", direction = Sort.Direction.DESC) Pageable p) {
        return service.all(p);
    }

    /**
     * Loads the requested record through an owner-scoped lookup; an absent ID and another user's ID produce the same not-found result.
     */
    @GetMapping("/{id}")
    public TransferView one(@PathVariable UUID id) {
        return service.one(id);
    }

    /**
     * Performs a full replacement of the transfer's editable fields. Clients should send the complete intended transfer
     * state; optional conversion/reference fields are replaced according to the service's null/default rules, not merged
     * with stored values. The owner-scoped lookup preserves the existing transfer ID and owner.
     */
    @PutMapping("/{id}")
    public TransferView update(@PathVariable UUID id, @Valid @RequestBody TransferRequest r) {
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
