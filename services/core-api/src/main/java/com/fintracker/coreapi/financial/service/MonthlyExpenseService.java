package com.fintracker.coreapi.financial.service;

import com.fintracker.coreapi.financial.dto.request.MonthlyExpenseRequest;
import com.fintracker.coreapi.financial.dto.response.MonthlyExpenseResponse;
import com.fintracker.coreapi.financial.entity.MonthlyExpense;
import com.fintracker.coreapi.financial.exception.MonthlyExpenseNotFoundException;
import com.fintracker.coreapi.financial.repository.MonthlyExpenseRepository;
import com.fintracker.coreapi.identity.service.AuthenticatedUserProvider;
import java.util.Locale;
import java.util.UUID;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Transactional use cases for monthly expenses, scoped to the JWT subject. */
@Service
public class MonthlyExpenseService {

    private final MonthlyExpenseRepository repository;
    private final AuthenticatedUserProvider authenticatedUserProvider;

    /**
     * Receives the repository and authenticated-user provider required to execute tenant-scoped use cases.
     */
    public MonthlyExpenseService(
            MonthlyExpenseRepository repository, AuthenticatedUserProvider authenticatedUserProvider) {
        this.repository = repository;
        this.authenticatedUserProvider = authenticatedUserProvider;
    }

    /**
     * Creates a record for the authenticated user, applies the domain's defaults and normalization rules, persists it, and maps the saved entity to its API response.
     */
    @Transactional
    public MonthlyExpenseResponse create(MonthlyExpenseRequest request) {
        UUID userId = authenticatedUserProvider.getRequiredUserId();
        MonthlyExpense entity =
                new MonthlyExpense(userId, request.expenseName().trim(), request.amount(), request.expenseDate());

        apply(entity, request);
        return MonthlyExpenseResponse.from(repository.save(entity));
    }

    /**
     * Returns a paginated monthly-expense collection for the authenticated user; filtering occurs in the repository query.
     */
    @Transactional(readOnly = true)
    public Page<MonthlyExpenseResponse> getAll(Pageable pageable) {
        UUID userId = authenticatedUserProvider.getRequiredUserId();
        FinancialPageableValidator.validate(pageable, Set.of("expenseDate", "amount", "expenseName", "category", "createdAt", "updatedAt"));
        return repository.findAllByUserId(userId, pageable).map(MonthlyExpenseResponse::from);
    }

    /**
     * Returns the requested expense only when it belongs to the authenticated user; a foreign-owned ID is treated as not found.
     */
    @Transactional(readOnly = true)
    public MonthlyExpenseResponse getById(UUID id) {
        return MonthlyExpenseResponse.from(findOwned(id));
    }

    /**
     * Loads the record within the caller's ownership scope, applies the replacement fields, and returns the updated response without changing its owner or ID.
     */
    @Transactional
    public MonthlyExpenseResponse update(UUID id, MonthlyExpenseRequest request) {
        MonthlyExpense entity = findOwned(id);

        apply(entity, request);
        return MonthlyExpenseResponse.from(entity);
    }

    /**
     * Deletes the record only after an owner-scoped lookup, preventing cross-user deletion.
     */
    @Transactional
    public void delete(UUID id) {
        repository.delete(findOwned(id));
    }

    /**
     * Uses the current authenticated UUID together with the record ID in the repository query, so ownership is enforced by the database predicate.
     */
    private MonthlyExpense findOwned(UUID id) {
        UUID userId = authenticatedUserProvider.getRequiredUserId();
        return repository.findByIdAndUserId(id, userId).orElseThrow(MonthlyExpenseNotFoundException::new);
    }

    /**
     * Normalizes user-entered values and applies documented defaults before updating the entity; sharing this method keeps create and update behavior aligned.
     */
    private void apply(MonthlyExpense entity, MonthlyExpenseRequest request) {
        String currency = request.currency() == null || request.currency().isBlank()
                ? "INR"
                : request.currency().trim().toUpperCase(Locale.ROOT);
        entity.updateDetails(
                request.expenseName().trim(),
                request.amount(),
                currency,
                request.exchangeRate(),
                request.inrEquivalent(),
                request.category(),
                request.expenseDate(),
                request.paymentMethod(),
                request.notes());
    }
}
