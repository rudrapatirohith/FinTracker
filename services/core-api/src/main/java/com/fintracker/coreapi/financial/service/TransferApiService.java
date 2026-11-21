package com.fintracker.coreapi.financial.service;

import com.fintracker.coreapi.financial.dto.request.TransferRequest;
import com.fintracker.coreapi.financial.dto.response.TransferView;
import com.fintracker.coreapi.financial.entity.InternationalTransfer;
import com.fintracker.coreapi.financial.repository.InternationalTransferRepository;
import com.fintracker.coreapi.identity.service.AuthenticatedUserProvider;
import java.math.BigDecimal;
import java.util.Locale;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Application service for international transfers. It derives the owner from the JWT, performs owner-scoped lookups, normalizes currencies/status, and delegates legacy currency-column synchronization to the entity.
 */
@Service
public class TransferApiService {
    private final InternationalTransferRepository repo;
    private final AuthenticatedUserProvider users;

    public TransferApiService(InternationalTransferRepository r, AuthenticatedUserProvider u) {
        repo = r;
        users = u;
    }

    /**
     * Creates a record for the authenticated user, applies the request's normalization rules, persists it, and returns the response projection.
     */
    @Transactional
    public TransferView create(TransferRequest r) {
        UUID uid = users.getRequiredUserId();
        InternationalTransfer e = new InternationalTransfer(
                uid,
                r.recipientName().trim(),
                r.recipientCountry().trim(),
                r.amountSent(),
                r.amountReceived(),
                r.exchangeRate(),
                r.transferDate());

        apply(e, r);
        return TransferView.from(repo.save(e));
    }

    /**
     * Returns a paginated collection belonging only to the authenticated user; the Pageable carries the requested page and sort.
     */
    @Transactional(readOnly = true)
    public Page<TransferView> all(Pageable p) {
        return repo.findAllByUserId(users.getRequiredUserId(), p).map(TransferView::from);
    }

    /**
     * Loads the requested record through an owner-scoped lookup; an absent ID and another user's ID produce the same not-found result.
     */
    @Transactional(readOnly = true)
    public TransferView one(UUID id) {
        return TransferView.from(owned(id));
    }

    /**
     * Replaces the record's editable business fields after an owner-scoped lookup, while preserving its ID and owner.
     */
    @Transactional
    public TransferView update(UUID id, TransferRequest r) {
        InternationalTransfer e = owned(id);

        apply(e, r);
        return TransferView.from(e);
    }

    /**
     * Deletes the record only after an owner-scoped lookup, preventing a caller from deleting another user's data.
     */
    @Transactional
    public void delete(UUID id) {
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
     * Trims recipient details, uppercases both currency codes, defaults a missing transfer fee to zero, normalizes status to lowercase, and synchronizes the currency fields through the entity update method.
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
