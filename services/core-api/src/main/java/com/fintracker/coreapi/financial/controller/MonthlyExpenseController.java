package com.fintracker.coreapi.financial.controller;

import com.fintracker.coreapi.financial.dto.request.MonthlyExpenseRequest;
import com.fintracker.coreapi.financial.dto.response.MonthlyExpenseResponse;
import com.fintracker.coreapi.financial.service.MonthlyExpenseService;
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

/** HTTP adapter for monthly expense use cases. */
@RestController
@RequestMapping("/api/v1/monthly-expenses")
public class MonthlyExpenseController {

    private final MonthlyExpenseService service;

    /**
     * Receives the application service/provider used by this controller; constructor injection keeps the dependency explicit and immutable.
     */
    public MonthlyExpenseController(MonthlyExpenseService service) {
        this.service = service;
    }

    /**
     * Creates a record for the authenticated user, applies the domain's defaults and normalization rules, persists it, and maps the saved entity to its API response.
     */
    @PostMapping
    public ResponseEntity<MonthlyExpenseResponse> create(@Valid @RequestBody MonthlyExpenseRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    /**
     * Returns the authenticated user's monthly expenses as a page, defaulting to the newest expense date first.
     */
    @GetMapping
    public Page<MonthlyExpenseResponse> getAll(
            @PageableDefault(size = 20, sort = "expenseDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return service.getAll(pageable);
    }

    /**
     * Returns the requested monthly expense through the owner-scoped service lookup; foreign-owned IDs are not disclosed.
     */
    @GetMapping("/{id}")
    public MonthlyExpenseResponse getById(@PathVariable UUID id) {
        return service.getById(id);
    }

    /**
     * Performs a full replacement of the expense's editable fields rather than a partial merge. Send the complete intended
     * expense state; optional values are replaced or cleared according to the service's null/default handling. The
     * owner-scoped lookup preserves the existing record ID and owner.
     */
    @PutMapping("/{id}")
    public MonthlyExpenseResponse update(@PathVariable UUID id, @Valid @RequestBody MonthlyExpenseRequest request) {
        return service.update(id, request);
    }

    /**
     * Deletes the record only after an owner-scoped lookup, preventing cross-user deletion.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
