package com.fintracker.coreapi.financial.service;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fintracker.coreapi.financial.dto.request.TransactionRequest;
import com.fintracker.coreapi.financial.dto.response.TransactionResponse;
import com.fintracker.coreapi.financial.entity.FinancialTransaction;
import com.fintracker.coreapi.financial.exception.TransactionNotFoundException;
import com.fintracker.coreapi.financial.repository.FinancialTransactionRepository;
import com.fintracker.coreapi.identity.service.AuthenticatedUserProvider;

/**
 * Owns transaction use cases and enforces tenant isolation at every lookup.
 * Never accepts a user ID from request data.
 */
@Service
public class TransactionService {

    private final FinancialTransactionRepository repository;
    private final AuthenticatedUserProvider authenticatedUserProvider;

    public TransactionService(
            FinancialTransactionRepository repository,
            AuthenticatedUserProvider authenticatedUserProvider) {
        this.repository = repository;
        this.authenticatedUserProvider = authenticatedUserProvider;
    }

    @Transactional
    public TransactionResponse create(TransactionRequest request) {
        UUID userId = authenticatedUserProvider.getRequiredUserId();
        FinancialTransaction entity = new FinancialTransaction(
                userId, request.type().trim(), request.category().trim(),
                request.amount(), request.transactionDate()
        );
        apply(entity, request);
        return TransactionResponse.from(repository.save(entity));
    }

    @Transactional(readOnly = true)
    public Page<TransactionResponse> getAll(Pageable pageable) {
        UUID userId = authenticatedUserProvider.getRequiredUserId();
        return repository.findAllByUserId(userId, pageable)
                .map(TransactionResponse::from);
    }

    @Transactional(readOnly = true)
    public TransactionResponse getById(UUID id) {
        return TransactionResponse.from(findOwned(id));
    }

    @Transactional
    public TransactionResponse update(UUID id, TransactionRequest request) {
        FinancialTransaction entity = findOwned(id);
        apply(entity, request);
        return TransactionResponse.from(entity);
    }

    @Transactional
    public void delete(UUID id) {
        repository.delete(findOwned(id));
    }

    private FinancialTransaction findOwned(UUID id) {
        UUID userId = authenticatedUserProvider.getRequiredUserId();
        return repository.findByIdAndUserId(id, userId)
                .orElseThrow(TransactionNotFoundException::new);
    }

    private void apply(FinancialTransaction entity, TransactionRequest request) {
        entity.updateDetails(
                request.type().trim(),
                request.category().trim(),
                request.subcategory(),
                request.amount(),
                normalizedCurrency(request.currency()),
                request.description(),
                request.merchant(),
                request.transactionDate(),
                request.paymentMethod(),
                request.referenceId(),
                request.tags()
        );
    }

    private String normalizedCurrency(String currency) {
        return currency == null || currency.isBlank()
                ? "USD"
                : currency.trim().toUpperCase(java.util.Locale.ROOT);
    }
}
