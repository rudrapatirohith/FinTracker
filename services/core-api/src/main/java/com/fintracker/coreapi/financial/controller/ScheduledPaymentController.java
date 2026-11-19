package com.fintracker.coreapi.financial.controller;

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

import com.fintracker.coreapi.financial.dto.request.ScheduledPaymentRequest;
import com.fintracker.coreapi.financial.dto.response.ScheduledPaymentResponse;
import com.fintracker.coreapi.financial.service.ScheduledPaymentService;

import jakarta.validation.Valid;

/** HTTP adapter for scheduled-payment operations. */
@RestController
@RequestMapping("/api/v1/scheduled-payments")
public class ScheduledPaymentController {

    private final ScheduledPaymentService service;

    public ScheduledPaymentController(ScheduledPaymentService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ScheduledPaymentResponse> create(
            @Valid @RequestBody ScheduledPaymentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @GetMapping
    public Page<ScheduledPaymentResponse> getAll(
            @PageableDefault(size = 20, sort = "dueDate", direction = Sort.Direction.ASC)
            Pageable pageable) {
        return service.getAll(pageable);
    }

    @GetMapping("/{id}")
    public ScheduledPaymentResponse getById(@PathVariable UUID id) {
        return service.getById(id);
    }

    @PutMapping("/{id}")
    public ScheduledPaymentResponse update(
            @PathVariable UUID id,
            @Valid @RequestBody ScheduledPaymentRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
