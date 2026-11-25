package com.fintracker.coreapi.financial.service;

import com.fintracker.coreapi.financial.dto.request.TransactionRequest;
import com.fintracker.coreapi.financial.dto.response.TransactionResponse;
import com.fintracker.coreapi.financial.entity.FinancialTransaction;
import com.fintracker.coreapi.financial.exception.TransactionNotFoundException;
import com.fintracker.coreapi.financial.repository.FinancialTransactionRepository;
import com.fintracker.coreapi.identity.service.AuthenticatedUserProvider;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

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

        apply(entity, request, userId);
        return TransactionResponse.from(repository.save(entity));
    }

    /**
     * Returns a paginated transaction list filtered by the authenticated user's UUID, keeping collection reads tenant-scoped at the repository layer.
     */
    @Transactional(readOnly = true)
    public Page<TransactionResponse> getAll(Pageable pageable) {
        UUID userId = authenticatedUserProvider.getRequiredUserId();
        FinancialPageableValidator.validate(
                pageable, Set.of("transactionDate", "amount", "type", "category", "createdAt", "updatedAt"));
        return repository.findAllByUserId(userId, pageable).map(TransactionResponse::from);
    }

    /**
     * Loads a transaction by ID and authenticated owner; foreign-owned and missing records produce the same not-found exception.
     */
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

        apply(entity, request, entity.getUserId());
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
     * Finds a transaction by both its ID and the current user's UUID; a missing row and a row owned by another user produce the same not-found exception.
     */
    private FinancialTransaction findOwned(UUID id) {
        UUID userId = authenticatedUserProvider.getRequiredUserId();
        return repository.findByIdAndUserId(id, userId).orElseThrow(TransactionNotFoundException::new);
    }

    /**
     * Trims the required type/category fields, normalizes a missing currency to USD or uppercases the supplied code, and passes tags through the request's defensive-copy accessor before updating the entity.
     */
    private void apply(FinancialTransaction entity, TransactionRequest request, UUID userId) {
        // A null reference on update means "leave the existing source link unchanged".
        // This prevents an ordinary edit from turning a history mirror into a duplicate event.
        UUID referenceId = request.referenceId() == null ? entity.getReferenceId() : request.referenceId();
        if (request.referenceId() != null) {
            validateReference(userId, referenceId);
        }
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
                referenceId,
                request.tags());
    }

    /**
     * A non-null referenceId represents a history mirror of an existing domain event.
     * Verify that the referenced row belongs to this same user before persisting it.
     */
    private void validateReference(UUID userId, UUID referenceId) {
        if (referenceId != null && !repository.existsCanonicalSourceForUser(referenceId, userId)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "referenceId must identify a financial source record owned by the signed-in user.");
        }
    }

    /**
     * Defaults a missing currency to USD and otherwise trims and uppercases it with Locale.ROOT to avoid machine-locale-dependent results.
     */
    private String normalizedCurrency(String currency) {
        return currency == null || currency.isBlank() ? "USD" : currency.trim().toUpperCase(java.util.Locale.ROOT);
    }
}
