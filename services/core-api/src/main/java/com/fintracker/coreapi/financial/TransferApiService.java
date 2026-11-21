package com.fintracker.coreapi.financial;

import com.fintracker.coreapi.financial.entity.*;
import com.fintracker.coreapi.financial.repository.*;
import com.fintracker.coreapi.identity.service.AuthenticatedUserProvider;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.Locale;
import java.util.UUID;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TransferApiService {
    private final InternationalTransferRepository repo;
    private final AuthenticatedUserProvider users;

  /**
   * Receives the repositories and authenticated-user provider required by this use case; dependencies are constructor-injected and retained for the service lifetime.
   */
    TransferApiService(InternationalTransferRepository r, AuthenticatedUserProvider u) {
        repo = r;
        users = u;
    }

    /**
     * Creates a record for the authenticated user, applies the request's normalization rules, persists it, and returns the response projection.
     */
    @Transactional
    TransferView create(TransferRequest r) {
        UUID uid = users.getRequiredUserId();
        InternationalTransfer e = new InternationalTransfer(
                uid,
                r.recipientName().trim(),
                r.recipientCountry().trim(),
                r.amountSent(),
                r.amountReceived(),
                r.exchangeRate(),
                r.transferDate());
      /**
       * Copies request values into the entity and centralizes trimming, currency normalization, defaults, and cross-field validation so create and update follow identical rules.
       */
        apply(e, r);
        return TransferView.from(repo.save(e));
    }

    /**
     * Returns a paginated collection belonging only to the authenticated user; the Pageable carries the requested page and sort.
     */
    @Transactional(readOnly = true)
    Page<TransferView> all(Pageable p) {
        return repo.findAllByUserId(users.getRequiredUserId(), p).map(TransferView::from);
    }

    /**
     * Loads the requested record through an owner-scoped lookup; an absent ID and another user's ID produce the same not-found result.
     */
    @Transactional(readOnly = true)
    TransferView one(UUID id) {
        return TransferView.from(owned(id));
    }

    /**
     * Replaces the record's editable business fields after an owner-scoped lookup, while preserving its ID and owner.
     */
    @Transactional
    TransferView update(UUID id, TransferRequest r) {
        InternationalTransfer e = owned(id);
      /**
       * Copies request values into the entity and centralizes trimming, currency normalization, defaults, and cross-field validation so create and update follow identical rules.
       */
        apply(e, r);
        return TransferView.from(e);
    }

    /**
     * Deletes the record only after an owner-scoped lookup, preventing a caller from deleting another user's data.
     */
    @Transactional
    void delete(UUID id) {
        repo.delete(owned(id));
    }

    /**
     * Resolves the authenticated user's ID and delegates to the owner-scoped repository lookup.
     */
    private InternationalTransfer owned(UUID id) {
        return repo.findByIdAndUserId(id, users.getRequiredUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transfer not found."));
    }

    /**
     * Copies request values into the entity and centralizes trimming, currency normalization, defaults, and cross-field validation so create and update follow identical rules.
     */
    private void apply(InternationalTransfer e, TransferRequest r) {
        String a = r.currencySent().toUpperCase(Locale.ROOT),
                b = r.currencyReceived().toUpperCase(Locale.ROOT);
        e.updateDetails(
                r.recipientName().trim(),
                r.recipientCountry().trim(),
                r.amountSent(),
                a,
                r.amountReceived(),
                b,
                r.exchangeRate(),
                r.transferFee() == null ? BigDecimal.ZERO : r.transferFee(),
                r.transferMethod(),
                r.purpose(),
                r.status().trim().toLowerCase(Locale.ROOT),
                r.transferDate(),
                r.completionDate(),
                r.referenceNumber(),
                r.usdToInrRate(),
                r.inrEquivalent());
    }
}
