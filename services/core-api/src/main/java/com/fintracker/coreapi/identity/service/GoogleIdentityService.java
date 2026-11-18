
package com.fintracker.coreapi.identity.service;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fintracker.coreapi.identity.entity.AppUser;
import com.fintracker.coreapi.identity.entity.OAuthIdentity;
import com.fintracker.coreapi.identity.entity.OAuthProvider;
import com.fintracker.coreapi.identity.entity.UserProfile;
import com.fintracker.coreapi.identity.entity.UserStatus;
import com.fintracker.coreapi.identity.exception.InvalidOAuthIdentityException;
import com.fintracker.coreapi.identity.exception.OAuthAccountLinkRequiredException;
import com.fintracker.coreapi.identity.repository.AppUserRepository;
import com.fintracker.coreapi.identity.repository.OAuthIdentityRepository;
import com.fintracker.coreapi.identity.repository.UserProfileRepository;

import org.springframework.security.oauth2.core.oidc.user.OidcUser;

/**
 * Resolves a verified Google identity to a FinTracker user.
 *
 * Google is responsible for authenticating the external identity.
 * FinTracker is responsible for its own user accounts.
 */
@Service
public class GoogleIdentityService {

    private final AppUserRepository appUserRepository;
    private final OAuthIdentityRepository oauthIdentityRepository;
    private final UserProfileRepository userProfileRepository;

    public GoogleIdentityService(
            AppUserRepository appUserRepository,
            OAuthIdentityRepository oauthIdentityRepository,
            UserProfileRepository userProfileRepository) {

        this.appUserRepository = appUserRepository;
        this.oauthIdentityRepository = oauthIdentityRepository;
        this.userProfileRepository = userProfileRepository;
    }

    /**
     * Finds an existing Google-linked user or creates a new account.
     *
     * Existing password accounts are never automatically linked
     * based solely on an email match.
     */
    @Transactional
    public AppUser resolveGoogleUser(OidcUser oidcUser) {

        if (oidcUser == null) {
            throw new InvalidOAuthIdentityException();
        }

        // Google OIDC's subject is the stable provider identity.
        String providerSubject = oidcUser.getSubject();

        String email = oidcUser.getClaimAsString("email");

        Boolean emailVerified =
            oidcUser.getClaim("email_verified");

        String fullName = oidcUser.getClaimAsString("name");

        if (providerSubject == null || providerSubject.isBlank()
                || email == null || email.isBlank()
                || !Boolean.TRUE.equals(emailVerified)) {

            throw new InvalidOAuthIdentityException();
        }

        String normalizedEmail =
            email.trim().toLowerCase(Locale.ROOT);

        /*
         * First check whether this exact Google identity
         * has already been connected to a FinTracker account.
         */
        OAuthIdentity existingIdentity =
            oauthIdentityRepository
                .findByProviderAndProviderSubject(
                    OAuthProvider.GOOGLE,
                    providerSubject
                )
                .orElse(null);

        if (existingIdentity != null) {

            AppUser existingUser = existingIdentity.getUser();

            if (existingUser.getStatus() != UserStatus.ACTIVE) {
                throw new InvalidOAuthIdentityException();
            }

            return existingUser;
        }

        /*
         * A matching email does not prove that the user controls
         * the existing FinTracker account.
         *
         * Require a separate authenticated account-linking flow.
         */
        if (appUserRepository.existsByEmailNormalized(normalizedEmail)) {
            throw new OAuthAccountLinkRequiredException();
        }

        Instant now = Instant.now();

        // Create a Google-only FinTracker account.
        // No password is stored for this account.
        AppUser newUser = new AppUser(
            UUID.randomUUID(),
            email,
            null
        );

        newUser.markEmailVerified(now);

        AppUser savedUser =
            appUserRepository.saveAndFlush(newUser);

        // Create the user's FinTracker profile.
        UserProfile profile =
            new UserProfile(savedUser, fullName);

        userProfileRepository.save(profile);

        // Connect the external Google subject to our internal user ID.
        OAuthIdentity identity = new OAuthIdentity(
            UUID.randomUUID(),
            savedUser,
            OAuthProvider.GOOGLE,
            providerSubject,
            email
        );

        oauthIdentityRepository.save(identity);

        return savedUser;
    }
}
