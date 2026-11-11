package com.fintracker.coreapi.identity.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "user_profiles")
public class UserProfile {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "user_id")
    private AppUser user;

    @Column(name = "full_name", length = 200)
    private String fullName;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Column(name = "currency_preference", nullable = false, length = 3)
    private String currencyPreference;

    @Column(nullable = false, length = 64)
    private String timezone;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected UserProfile() {
        // Required by JPA.
    }

    public UserProfile(AppUser user, String fullName) {
        if (user == null) {
            throw new IllegalArgumentException("User is required");
        }

        this.user = user;
        this.fullName = fullName;
        this.currencyPreference = "USD";
        this.timezone = "UTC";
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public UUID getUserId() {
        return userId;
    }

    public AppUser getUser() {
        return user;
    }

    public String getFullName() {
        return fullName;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public String getCurrencyPreference() {
        return currencyPreference;
    }

    public String getTimezone() {
        return timezone;
    }
}