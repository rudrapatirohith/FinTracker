package com.fintracker.coreapi.financial.service;

import com.fintracker.coreapi.financial.dto.request.TransactionRequest;
import com.fintracker.coreapi.financial.dto.response.TransactionResponse;
import com.fintracker.coreapi.financial.entity.FinancialTransaction;
import com.fintracker.coreapi.financial.exception.TransactionNotFoundException;
import com.fintracker.coreapi.financial.repository.FinancialTransactionRepository;
import com.fintracker.coreapi.identity.service.AuthenticatedUserProvider;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Owns transaction use cases and enforces tenant isolation at every lookup.
 * Never accepts a user ID from request data.
 */
@Service
public class TransactionService {

    private final FinancialTransactionRepository repository;
    private final AuthenticatedUserProvider authenticatedUserProvider;

    /**
     * Receives the persistence repository and authenticated-user provider so each use case can enforce tenant scope without trusting request-supplied ownership.
     */
    public TransactionService(
            FinancialTransactionRepository repository, AuthenticatedUserProvider authenticatedUserProvider) {
        this.repository = repository;
        this.authenticatedUserProvider = authenticatedUserProvider;
    }

    /**
     * Creates a record for the authenticated user, applies the request's normalization rules, persists it, and returns the response projection.
     */
    @Transactional
    public TransactionResponse create(TransactionRequest request) {
        UUID userId = authenticatedUserProvider.getRequiredUserId();
        FinancialTransaction entity = new FinancialTransaction(
                userId, request.type().trim(), request.category().trim(), request.amount(), request.transactionDate());
      /**
       * Copies request values into the entity and centralizes trimming, currency normalization, defaults, and cross-field validation so create and update follow identical rules.
       */
        apply(entity, request);
        return TransactionResponse.from(repository.save(entity));
    }

    @Transactional(readOnly = true)
    public Page<TransactionResponse> getAll(Pageable pageable) {
        UUID userId = authenticatedUserProvider.getRequiredUserId();
        return repository.findAllByUserId(userId, pageable).map(TransactionResponse::from);
    }

    @Transactional(readOnly = true)
    public TransactionResponse getById(UUID id) {
        return TransactionResponse.from(findOwned(id));
    }

    /**
     * Replaces the record's editable business fields after an owner-scoped lookup, while preserving its ID and owner.
     */
    @Transactional
    public TransactionResponse update(UUID id, TransactionRequest request) {
        FinancialTransaction entity = findOwned(id);
      /**
       * Copies request values into the entity and centralizes trimming, currency normalization, defaults, and cross-field validation so create and update follow identical rules.
       */
        apply(entity, request);
        return TransactionResponse.from(entity);
    }

    /**
     * Deletes the record only after an owner-scoped lookup, preventing a caller from deleting another user's data.
     */
    @Transactional
    public void delete(UUID id) {
        repository.delete(findOwned(id));
    }

    /**
     * Performs the find owned step for this use case.
     */
    private FinancialTransaction findOwned(UUID id) {
        UUID userId = authenticatedUserProvider.getRequiredUserId();
        return repository.findByIdAndUserId(id, userId).orElseThrow(TransactionNotFoundException::new);
    }

    /**
     * Copies request values into the entity and centralizes trimming, currency normalization, defaults, and cross-field validation so create and update follow identical rules.
     */
    private void apply(FinancialTransaction entity, TransactionRequest request) {
        entity.updateDetails(
                request.type().trim(),
                request.category().trim(),
                request.subcategory(),
                request.amount(),
              /**
               * Defaults a missing currency to USD and otherwise trims and uppercases it with Locale.ROOT to avoid machine-locale-dependent results.
               */
                normalizedCurrency(request.currency()),
                request.description(),
                request.merchant(),
                request.transactionDate(),
                request.paymentMethod(),
                request.referenceId(),
                request.tags());
    }

    /**
     * Defaults a missing currency to USD and otherwise trims and uppercases it with Locale.ROOT to avoid machine-locale-dependent results.
     */
    private String normalizedCurrency(String currency) {
        return currency == null || currency.isBlank() ? "USD" : currency.trim().toUpperCase(java.util.Locale.ROOT);
    }
}
