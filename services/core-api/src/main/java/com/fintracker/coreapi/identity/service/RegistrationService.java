package com.fintracker.coreapi.identity.service;

import java.util.Locale;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fintracker.coreapi.identity.dto.request.RegisterRequest;
import com.fintracker.coreapi.identity.dto.response.RegisterResponse;
import com.fintracker.coreapi.identity.entity.AppUser;
import com.fintracker.coreapi.identity.entity.UserProfile;
import com.fintracker.coreapi.identity.exception.EmailAlreadyRegisteredException;
import com.fintracker.coreapi.identity.repository.AppUserRepository;
import com.fintracker.coreapi.identity.repository.UserProfileRepository;

/**
 * Handles user registration business logic.
 */
@Service
public class RegistrationService {

    private final AppUserRepository appUserRepository;
    private final UserProfileRepository userProfileRepository;
    private final PasswordEncoder passwordEncoder;

    public RegistrationService(
            AppUserRepository appUserRepository,
            UserProfileRepository userProfileRepository,
            PasswordEncoder passwordEncoder) {

        this.appUserRepository = appUserRepository;
        this.userProfileRepository = userProfileRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Creates a user and their profile atomically.
     */
    @Transactional
    public RegisterResponse register(RegisterRequest request) {

        // 1. Normalize email before checking uniqueness.
        String normalizedEmail = request.email()
            .trim()
            .toLowerCase(Locale.ROOT);

        // 2. Reject an existing account.
        if (appUserRepository.existsByEmailNormalized(normalizedEmail)) {
            throw new EmailAlreadyRegisteredException();
        }

        // 3. Hash the password. Never persist the original password.
        String passwordHash = passwordEncoder.encode(request.password());

        // 4. Create the user.
        AppUser user = new AppUser(
            UUID.randomUUID(),
            request.email().trim(),
            passwordHash
        );

        AppUser savedUser = appUserRepository.save(user);

        // 5. Create the associated profile.
        UserProfile profile = new UserProfile(
            savedUser,
            request.fullName().trim()
        );

        userProfileRepository.save(profile);

        // 6. Return only safe user information.
        return new RegisterResponse(
            savedUser.getId(),
            savedUser.getEmail(),
            profile.getFullName(),
            savedUser.getStatus()
        );
    }
}