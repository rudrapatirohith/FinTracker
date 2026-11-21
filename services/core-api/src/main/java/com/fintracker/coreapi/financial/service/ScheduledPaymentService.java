package com.fintracker.coreapi.financial.service;

import com.fintracker.coreapi.financial.dto.request.ScheduledPaymentRequest;
import com.fintracker.coreapi.financial.dto.response.ScheduledPaymentResponse;
import com.fintracker.coreapi.financial.entity.ScheduledPayment;
import com.fintracker.coreapi.financial.exception.ScheduledPaymentNotFoundException;
import com.fintracker.coreapi.financial.repository.ScheduledPaymentRepository;
import com.fintracker.coreapi.identity.service.AuthenticatedUserProvider;
import java.util.Locale;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Use cases for scheduled payments with user ownership enforced server-side. */
@Service
public class ScheduledPaymentService {

    private final ScheduledPaymentRepository repository;
    private final AuthenticatedUserProvider authenticatedUserProvider;

    /**
     * Receives the repository and authenticated-user provider required to execute tenant-scoped use cases.
     */
    public ScheduledPaymentService(
            ScheduledPaymentRepository repository, AuthenticatedUserProvider authenticatedUserProvider) {
        this.repository = repository;
        this.authenticatedUserProvider = authenticatedUserProvider;
    }

    /**
     * Creates a record for the authenticated user, applies the domain's defaults and normalization rules, persists it, and maps the saved entity to its API response.
     */
    @Transactional
    public ScheduledPaymentResponse create(ScheduledPaymentRequest request) {
        UUID userId = authenticatedUserProvider.getRequiredUserId();
        ScheduledPayment entity =

                new ScheduledPayment(userId, request.paymentName().trim(), request.amount(), request.dueDate());

        apply(entity, request);
        return ScheduledPaymentResponse.from(repository.save(entity));
    }

    /**
     * Returns a paginated list of scheduled payments belonging to the authenticated user, with the requested page and sort delegated to Spring Data.
     */
    @Transactional(readOnly = true)
    public Page<ScheduledPaymentResponse> getAll(Pageable pageable) {
        UUID userId = authenticatedUserProvider.getRequiredUserId();
        return repository.findAllByUserId(userId, pageable).map(ScheduledPaymentResponse::from);
    }

    /**
     * Returns a scheduled payment only when the ID is owned by the current user; missing and foreign-owned IDs share the same not-found result.
     */
    @Transactional(readOnly = true)
    public ScheduledPaymentResponse getById(UUID id) {
        return ScheduledPaymentResponse.from(findOwned(id));
    }

    /**
     * Loads the record within the caller's ownership scope, applies the replacement fields, and returns the updated response without changing its owner or ID.
     */
    @Transactional
    public ScheduledPaymentResponse update(UUID id, ScheduledPaymentRequest request) {
        ScheduledPayment entity = findOwned(id);

        apply(entity, request);
        return ScheduledPaymentResponse.from(entity);
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
    private ScheduledPayment findOwned(UUID id) {
        UUID userId = authenticatedUserProvider.getRequiredUserId();
        return repository.findByIdAndUserId(id, userId).orElseThrow(ScheduledPaymentNotFoundException::new);
    }

    /**
     * Normalizes user-entered values and applies documented defaults before updating the entity; sharing this method keeps create and update behavior aligned.
     */
    private void apply(ScheduledPayment entity, ScheduledPaymentRequest request) {
        String currency = request.currency() == null || request.currency().isBlank()
                ? "USD"
                : request.currency().trim().toUpperCase(Locale.ROOT);
        entity.updateDetails(
                request.paymentName().trim(),
                request.amount(),
                currency,
                request.category(),
                request.recipient(),
                request.dueDate(),
                request.frequency(),
                request.recurring() == null ? Boolean.FALSE : request.recurring(),
                request.autoPay() == null ? Boolean.FALSE : request.autoPay(),
                request.status() == null || request.status().isBlank()
                        ? "pending"
                        : request.status().trim().toLowerCase(Locale.ROOT),
                request.reminderDays() == null ? 3 : request.reminderDays(),
                request.notes());
    }
}
