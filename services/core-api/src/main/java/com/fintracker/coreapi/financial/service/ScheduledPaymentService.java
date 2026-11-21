/**
 * Transactional use cases for scheduled payments.
 * The service derives ownership from the authenticated JWT, scopes every record lookup to that owner, and centralizes defaults for currency, recurrence, autopay, status, and reminder days.
 * Read-only transactions are used for queries; write operations execute within transactions.
 */
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

    public ScheduledPaymentService(
            ScheduledPaymentRepository repository, AuthenticatedUserProvider authenticatedUserProvider) {
        this.repository = repository;
        this.authenticatedUserProvider = authenticatedUserProvider;
    }

    @Transactional
    public ScheduledPaymentResponse create(ScheduledPaymentRequest request) {
        UUID userId = authenticatedUserProvider.getRequiredUserId();
        ScheduledPayment entity =
                new ScheduledPayment(userId, request.paymentName().trim(), request.amount(), request.dueDate());
        apply(entity, request);
        return ScheduledPaymentResponse.from(repository.save(entity));
    }

    @Transactional(readOnly = true)
    public Page<ScheduledPaymentResponse> getAll(Pageable pageable) {
        UUID userId = authenticatedUserProvider.getRequiredUserId();
        return repository.findAllByUserId(userId, pageable).map(ScheduledPaymentResponse::from);
    }

    @Transactional(readOnly = true)
    public ScheduledPaymentResponse getById(UUID id) {
        return ScheduledPaymentResponse.from(findOwned(id));
    }

    @Transactional
    public ScheduledPaymentResponse update(UUID id, ScheduledPaymentRequest request) {
        ScheduledPayment entity = findOwned(id);
        apply(entity, request);
        return ScheduledPaymentResponse.from(entity);
    }

    @Transactional
    public void delete(UUID id) {
        repository.delete(findOwned(id));
    }

    private ScheduledPayment findOwned(UUID id) {
        UUID userId = authenticatedUserProvider.getRequiredUserId();
        return repository.findByIdAndUserId(id, userId).orElseThrow(ScheduledPaymentNotFoundException::new);
    }

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
