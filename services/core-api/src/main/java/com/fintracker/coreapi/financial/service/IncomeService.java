/**
 * Transactional application service for income records.
 * It obtains the owner exclusively from AuthenticatedUserProvider, uses owner-scoped repository queries for every ID operation, and maps entities to response DTOs.
 * Write methods run inside transactions; collection/detail reads are read-only.
 * applyRequest centralizes normalization and defaults so create and update behave consistently.
 */
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

    public IncomeService(IncomeRepository incomeRepository, AuthenticatedUserProvider authenticatedUserProvider) {
        this.incomeRepository = incomeRepository;
        this.authenticatedUserProvider = authenticatedUserProvider;
    }

    @Transactional
    public IncomeResponse create(IncomeRequest request) {
        UUID userId = authenticatedUserProvider.getRequiredUserId();

        Income income = new Income(userId, request.source().trim(), request.amount(), request.date());

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

    @Transactional
    public IncomeResponse update(UUID incomeId, IncomeRequest request) {
        UUID userId = authenticatedUserProvider.getRequiredUserId();

        Income income = findOwnedIncome(incomeId, userId);
        applyRequest(income, request);

        return IncomeResponse.from(incomeRepository.save(income));
    }

    @Transactional
    public void delete(UUID incomeId) {
        UUID userId = authenticatedUserProvider.getRequiredUserId();

        Income income = findOwnedIncome(incomeId, userId);
        incomeRepository.delete(income);
    }

    private Income findOwnedIncome(UUID incomeId, UUID userId) {
        return incomeRepository.findByIdAndUserId(incomeId, userId).orElseThrow(IncomeNotFoundException::new);
    }

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
