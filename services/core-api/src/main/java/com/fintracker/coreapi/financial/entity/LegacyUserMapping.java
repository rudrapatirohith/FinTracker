package com.fintracker.coreapi.financial.entity;

import com.fintracker.coreapi.identity.entity.AppUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "legacy_user_mappings",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uq_legacy_user_mappings_external",
                    columnNames = {"provider", "legacy_user_id"}),
            @UniqueConstraint(
                    name = "uq_legacy_user_mappings_app_user",
                    columnNames = {"provider", "app_user_id"})
        })
public class LegacyUserMapping {

    public static final String SUPABASE_PROVIDER = "SUPABASE";

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "provider", nullable = false, length = 30)
    private String provider;

    @Column(name = "legacy_user_id", nullable = false)
    private UUID legacyUserId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "app_user_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_legacy_user_mappings_app_user"))
    private AppUser appUser;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected LegacyUserMapping() {}

    public LegacyUserMapping(String provider, UUID legacyUserId, AppUser appUser) {
        this.provider = provider;
        this.legacyUserId = legacyUserId;
        this.appUser = appUser;
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public UUID getId() {
        return id;
    }

    public String getProvider() {
        return provider;
    }

    public UUID getLegacyUserId() {
        return legacyUserId;
    }

    public AppUser getAppUser() {
        return appUser;
    }

    public UUID getAppUserId() {
        return appUser != null ? appUser.getId() : null;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
