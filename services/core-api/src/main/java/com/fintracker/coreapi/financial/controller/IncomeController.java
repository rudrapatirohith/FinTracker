package com.fintracker.coreapi.financial.controller;

import com.fintracker.coreapi.financial.dto.request.IncomeRequest;
import com.fintracker.coreapi.financial.dto.response.IncomeResponse;
import com.fintracker.coreapi.financial.service.IncomeService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
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
 * HTTP boundary for the authenticated user's income records.
 *
 * <p>Ownership checks and transaction boundaries belong to the service layer;
 * this controller only validates input, delegates, and defines HTTP semantics.</p>
 */
@RestController
@RequestMapping("/api/v1/income")
public class IncomeController {

    private final IncomeService incomeService;

    /**
     * Receives the application service/provider used by this controller; constructor injection keeps the dependency explicit and immutable.
     */
    public IncomeController(IncomeService incomeService) {
        this.incomeService = incomeService;
    }

    /**
     * Creates a record for the authenticated user, applies the domain's defaults and normalization rules, persists it, and maps the saved entity to its API response.
     */
    @PostMapping
    public ResponseEntity<IncomeResponse> create(@Valid @RequestBody IncomeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(incomeService.create(request));
    }

    @GetMapping
    public Page<IncomeResponse> getAll(
            @PageableDefault(size = 20, sort = "date", direction = Sort.Direction.DESC) Pageable pageable) {
        return incomeService.getAll(pageable);
    }

    @GetMapping("/{id}")
    public IncomeResponse getById(@PathVariable UUID id) {
        return incomeService.getById(id);
    }

    /**
     * Loads the record within the caller's ownership scope, applies the replacement fields, and returns the updated response without changing its owner or ID.
     */
    @PutMapping("/{id}")
    public IncomeResponse update(@PathVariable UUID id, @Valid @RequestBody IncomeRequest request) {
        return incomeService.update(id, request);
    }

    /**
     * Deletes the record only after an owner-scoped lookup, preventing cross-user deletion.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        incomeService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
