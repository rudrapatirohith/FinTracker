package com.fintracker.coreapi.financial.service;

import java.util.Locale;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fintracker.coreapi.financial.dto.request.MonthlyExpenseRequest;
import com.fintracker.coreapi.financial.dto.response.MonthlyExpenseResponse;
import com.fintracker.coreapi.financial.entity.MonthlyExpense;
import com.fintracker.coreapi.financial.exception.MonthlyExpenseNotFoundException;
import com.fintracker.coreapi.financial.repository.MonthlyExpenseRepository;
import com.fintracker.coreapi.identity.service.AuthenticatedUserProvider;

/** Transactional use cases for monthly expenses, scoped to the JWT subject. */
@Service
public class MonthlyExpenseService {

    private final MonthlyExpenseRepository repository;
    private final AuthenticatedUserProvider authenticatedUserProvider;

    public MonthlyExpenseService(
            MonthlyExpenseRepository repository,
            AuthenticatedUserProvider authenticatedUserProvider) {
        this.repository = repository;
        this.authenticatedUserProvider = authenticatedUserProvider;
    }

    @Transactional
    public MonthlyExpenseResponse create(MonthlyExpenseRequest request) {
        UUID userId = authenticatedUserProvider.getRequiredUserId();
        MonthlyExpense entity = new MonthlyExpense(
                userId, request.expenseName().trim(), request.amount(), request.expenseDate());
        apply(entity, request);
        return MonthlyExpenseResponse.from(repository.save(entity));
    }

    @Transactional(readOnly = true)
    public Page<MonthlyExpenseResponse> getAll(Pageable pageable) {
        UUID userId = authenticatedUserProvider.getRequiredUserId();
        return repository.findAllByUserId(userId, pageable).map(MonthlyExpenseResponse::from);
    }

    @Transactional(readOnly = true)
    public MonthlyExpenseResponse getById(UUID id) {
        return MonthlyExpenseResponse.from(findOwned(id));
    }

    @Transactional
    public MonthlyExpenseResponse update(UUID id, MonthlyExpenseRequest request) {
        MonthlyExpense entity = findOwned(id);
        apply(entity, request);
        return MonthlyExpenseResponse.from(entity);
    }

    @Transactional
    public void delete(UUID id) {
        repository.delete(findOwned(id));
    }

    private MonthlyExpense findOwned(UUID id) {
        UUID userId = authenticatedUserProvider.getRequiredUserId();
        return repository.findByIdAndUserId(id, userId)
                .orElseThrow(MonthlyExpenseNotFoundException::new);
    }

    private void apply(MonthlyExpense entity, MonthlyExpenseRequest request) {
        String currency = request.currency() == null || request.currency().isBlank()
                ? "INR" : request.currency().trim().toUpperCase(Locale.ROOT);
        entity.updateDetails(
                request.expenseName().trim(), request.amount(), currency,
                request.exchangeRate(), request.inrEquivalent(), request.category(),
                request.expenseDate(), request.paymentMethod(), request.notes());
    }
}
