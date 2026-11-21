/**
 * REST adapter for monthly-expense operations.
 * It validates request bodies and translates service results into conventional HTTP responses; it deliberately contains no database or tenant logic.
 * The collection defaults to newest expense date first and remains paginated.
 */
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

    public MonthlyExpenseController(MonthlyExpenseService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<MonthlyExpenseResponse> create(@Valid @RequestBody MonthlyExpenseRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @GetMapping
    public Page<MonthlyExpenseResponse> getAll(
            @PageableDefault(size = 20, sort = "expenseDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return service.getAll(pageable);
    }

    @GetMapping("/{id}")
    public MonthlyExpenseResponse getById(@PathVariable UUID id) {
        return service.getById(id);
    }

    @PutMapping("/{id}")
    public MonthlyExpenseResponse update(@PathVariable UUID id, @Valid @RequestBody MonthlyExpenseRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
