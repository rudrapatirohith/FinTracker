package com.fintracker.coreapi.identity.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Represents a temporary, single-use authorization handoff.
 *
 * It connects a successful OAuth login to the frontend without
 * exposing the application's access JWT in a redirect URL.
 *
 * Only hashes of sensitive tokens are stored.
 */
@Entity
@Table(name = "oauth_login_handoffs")
public class OAuthLoginHandoff {

    @Id
    private UUID id;

    @Column(name = "code_hash", nullable = false, length = 64, unique = true)
    private String codeHash;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(name = "refresh_token_hash", nullable = false, length = 64)
    private String refreshTokenHash;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "consumed_at")
    private Instant consumedAt;

    protected OAuthLoginHandoff() {
        // Required by JPA.
    }

    public OAuthLoginHandoff(
            UUID id, String codeHash, AppUser user, String refreshTokenHash, Instant createdAt, Instant expiresAt) {

        if (id == null
                || codeHash == null
                || user == null
                || refreshTokenHash == null
                || createdAt == null
                || expiresAt == null
                || !expiresAt.isAfter(createdAt)) {

            throw new IllegalArgumentException("Valid OAuth handoff fields are required.");
        }

        this.id = id;
        this.codeHash = codeHash;
        this.user = user;
        this.refreshTokenHash = refreshTokenHash;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public boolean isExpired(Instant now) {
        return !expiresAt.isAfter(now);
    }

    public boolean isConsumed() {
        return consumedAt != null;
    }

    /**
     * Marks this handoff as consumed.
     */
    public void consume(Instant now) {
        if (now == null || isConsumed() || isExpired(now)) {
            throw new IllegalStateException("OAuth handoff cannot be consumed.");
        }

        this.consumedAt = now;
    }

    public UUID getId() {
        return id;
    }

    public String getCodeHash() {
        return codeHash;
    }

    public AppUser getUser() {
        return user;
    }

    public String getRefreshTokenHash() {
        return refreshTokenHash;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getConsumedAt() {
        return consumedAt;
    }
}
