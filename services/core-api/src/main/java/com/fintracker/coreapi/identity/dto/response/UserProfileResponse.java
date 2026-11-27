package com.fintracker.coreapi.identity.dto.response;

import com.fintracker.coreapi.identity.entity.AppUser;
import com.fintracker.coreapi.identity.entity.UserProfile;
import java.time.Instant;
import java.util.UUID;

/** Safe profile response; password hashes and authentication internals are never exposed. */
public record UserProfileResponse(
        UUID userId,
        String email,
        String fullName,
        String avatarUrl,
        String currencyPreference,
        String timezone,
        Instant accountCreatedAt,
        Instant profileUpdatedAt) {

    public static UserProfileResponse from(AppUser user, UserProfile profile) {
        return new UserProfileResponse(
                user.getId(),
                user.getEmail(),
                profile.getFullName(),
                profile.getAvatarUrl(),
                profile.getCurrencyPreference(),
                profile.getTimezone(),
                user.getCreatedAt(),
                profile.getUpdatedAt());
    }
}
