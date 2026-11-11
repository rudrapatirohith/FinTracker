package com.fintracker.coreapi.identity.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "user_sessions")
public class UserSession {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(
        name = "refresh_token_hash",
        nullable = false,
        unique = true,
        length = 64
    )
    private String refreshTokenHash;   

    @Column(name = "token_family_id", nullable = false)
    private UUID tokenFamilyId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "last_used_at")
    private Instant lastUsedAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "revocation_reason", length = 100)
    private String revocationReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "replaced_by_session_id")
    private UserSession replacedBySession;

    @Column(name = "user_agent")
    private String userAgent;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    protected UserSession() {
        // Required by JPA.
    }

    public UserSession(
            UUID id,
            AppUser user,
            String refreshTokenHash,
            UUID tokenFamilyId,
            Instant expiresAt) {

        if (id == null || user == null || refreshTokenHash == null
                || tokenFamilyId == null || expiresAt == null) {
            throw new IllegalArgumentException(
                "Session fields are required"
            );
        }

        this.id = id;
        this.user = user;
        this.refreshTokenHash = refreshTokenHash;
        this.tokenFamilyId = tokenFamilyId;
        this.createdAt = Instant.now();
        this.expiresAt = expiresAt;
    }

    public UUID getId() {
        return id;
    }

    public AppUser getUser() {
        return user;
    }

    public String getRefreshTokenHash() {
        return refreshTokenHash;
    }

    public UUID getTokenFamilyId() {
        return tokenFamilyId;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public UserSession getReplacedBySession() {
        return replacedBySession;
    }
}