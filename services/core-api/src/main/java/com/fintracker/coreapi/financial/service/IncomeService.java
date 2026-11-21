package com.fintracker.coreapi.financial.service;

import com.fintracker.coreapi.financial.dto.request.IncomeRequest;
import com.fintracker.coreapi.financial.dto.response.IncomeResponse;
import com.fintracker.coreapi.financial.entity.Income;
import com.fintracker.coreapi.financial.exception.IncomeNotFoundException;
import com.fintracker.coreapi.financial.repository.IncomeRepository;
import com.fintracker.coreapi.identity.service.AuthenticatedUserProvider;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

      /**
       * Copies request values into the income entity and centralizes currency/recurrence defaults so create and update behave consistently.
       */
        applyRequest(income, request);

        return IncomeResponse.from(incomeRepository.save(income));
    }

    @Transactional(readOnly = true)
    public Page<IncomeResponse> getAll(Pageable pageable) {
        UUID userId = authenticatedUserProvider.getRequiredUserId();

        return incomeRepository.findAllByUserId(userId, pageable).map(IncomeResponse::from);
    }

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
      /**
       * Copies request values into the income entity and centralizes currency/recurrence defaults so create and update behave consistently.
       */
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
