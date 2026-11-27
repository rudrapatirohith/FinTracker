package com.fintracker.coreapi.identity.service;

import com.fintracker.coreapi.identity.dto.request.UserProfileUpdateRequest;
import com.fintracker.coreapi.identity.dto.response.UserProfileResponse;
import com.fintracker.coreapi.identity.entity.AppUser;
import com.fintracker.coreapi.identity.entity.UserProfile;
import com.fintracker.coreapi.identity.repository.AppUserRepository;
import com.fintracker.coreapi.identity.repository.UserProfileRepository;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Owns the authenticated user's editable profile preferences. The caller's
 * identity always comes from the validated JWT, never from a request field.
 */
@Service
public class UserProfileService {
    private final UserProfileRepository profiles;
    private final AppUserRepository users;
    private final AuthenticatedUserProvider authenticatedUsers;

    public UserProfileService(
            UserProfileRepository profiles,
            AppUserRepository users,
            AuthenticatedUserProvider authenticatedUsers) {
        this.profiles = profiles;
        this.users = users;
        this.authenticatedUsers = authenticatedUsers;
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getCurrentProfile() {
        UUID userId = authenticatedUsers.getRequiredUserId();
        AppUser user = requireUser(userId);
        UserProfile profile = profiles.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User profile not found."));
        return UserProfileResponse.from(user, profile);
    }

    @Transactional
    public UserProfileResponse updateCurrentProfile(UserProfileUpdateRequest request) {
        UUID userId = authenticatedUsers.getRequiredUserId();
        AppUser user = requireUser(userId);
        UserProfile profile = profiles.findById(userId)
                .orElseGet(() -> profiles.save(new UserProfile(user, request.fullName())));
        profile.updatePreferences(request.fullName(), request.currencyPreference(), request.timezone());
        return UserProfileResponse.from(user, profiles.save(profile));
    }

    private AppUser requireUser(UUID userId) {
        return users.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found."));
    }
}
