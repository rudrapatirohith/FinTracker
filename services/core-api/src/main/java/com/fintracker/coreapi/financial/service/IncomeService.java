package com.fintracker.coreapi.financial.service;

import com.fintracker.coreapi.financial.dto.request.IncomeRequest;
import com.fintracker.coreapi.financial.dto.response.IncomeResponse;
import com.fintracker.coreapi.financial.entity.Income;
import com.fintracker.coreapi.financial.exception.IncomeNotFoundException;
import com.fintracker.coreapi.financial.repository.IncomeRepository;
import com.fintracker.coreapi.identity.service.AuthenticatedUserProvider;
import java.util.UUID;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application service for income CRUD. It derives the owner from Spring Security, scopes every record lookup by owner, applies the same defaults on create and update, and maps entities to response DTOs.
 */
@Service
public class IncomeService {

    private final IncomeRepository incomeRepository;
    private final AuthenticatedUserProvider authenticatedUserProvider;

    /**
     * Receives the persistence repository and authenticated-user provider so each use case can enforce tenant scope without trusting request-supplied ownership.
     */
    public IncomeService(IncomeRepository incomeRepository, AuthenticatedUserProvider authenticatedUserProvider) {
        this.incomeRepository = incomeRepository;
        this.authenticatedUserProvider = authenticatedUserProvider;
    }

    /**
     * Creates a record for the authenticated user, applies the request's normalization rules, persists it, and returns the response projection.
     */
    @Transactional
    public IncomeResponse create(IncomeRequest request) {
        UUID userId = authenticatedUserProvider.getRequiredUserId();

        Income income = new Income(userId, request.source().trim(), request.amount(), request.date());

        applyRequest(income, request);

        return IncomeResponse.from(incomeRepository.save(income));
    }

    /**
     * Returns a paginated list of income records owned by the authenticated user. The repository applies the owner filter in SQL, and the response mapper prevents persistence-only fields from leaving the service.
     */
    @Transactional(readOnly = true)
    public Page<IncomeResponse> getAll(Pageable pageable) {
        UUID userId = authenticatedUserProvider.getRequiredUserId();
        FinancialPageableValidator.validate(pageable, Set.of("date", "amount", "source", "category", "createdAt", "updatedAt"));
        return incomeRepository.findAllByUserId(userId, pageable).map(IncomeResponse::from);
    }

    /**
     * Returns one income record only when its ID belongs to the current user; an unknown ID and another user's ID both raise IncomeNotFoundException.
     */
    @Transactional(readOnly = true)
    public IncomeResponse getById(UUID incomeId) {
        UUID userId = authenticatedUserProvider.getRequiredUserId();

        return IncomeResponse.from(findOwnedIncome(incomeId, userId));
    }

    /**
     * Replaces the record's editable business fields after an owner-scoped lookup, while preserving its ID and owner.
     */
    @Transactional
    public IncomeResponse update(UUID incomeId, IncomeRequest request) {
        UUID userId = authenticatedUserProvider.getRequiredUserId();

        Income income = findOwnedIncome(incomeId, userId);

        applyRequest(income, request);

        return IncomeResponse.from(incomeRepository.save(income));
    }

    /**
     * Deletes the record only after an owner-scoped lookup, preventing a caller from deleting another user's data.
     */
    @Transactional
    public void delete(UUID incomeId) {
        UUID userId = authenticatedUserProvider.getRequiredUserId();

        Income income = findOwnedIncome(incomeId, userId);
        incomeRepository.delete(income);
    }

    /**
     * Queries by both income ID and authenticated owner ID; this avoids loading a foreign tenant's row before checking access.
     */
    private Income findOwnedIncome(UUID incomeId, UUID userId) {
        return incomeRepository.findByIdAndUserId(incomeId, userId).orElseThrow(IncomeNotFoundException::new);
    }

    /**
     * Copies request values into the income entity and centralizes currency/recurrence defaults so create and update behave consistently.
     */
    private void applyRequest(Income income, IncomeRequest request) {
        income.updateDetails(
                request.source().trim(),
                request.amount(),
                request.currency() == null || request.currency().isBlank()
                        ? "USD"
                        : request.currency().trim(),
                request.category(),
                request.description(),
                request.date(),
                request.recurring() == null ? Boolean.FALSE : request.recurring(),
                request.recurringFrequency(),
                request.exchangeRate(),
                request.inrEquivalent());
    }
}
