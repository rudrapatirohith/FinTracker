package com.fintracker.coreapi.identity.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import com.fintracker.coreapi.identity.dto.response.OAuthLoginCompletion;
import com.fintracker.coreapi.identity.entity.AppUser;

@Service
public class OAuthLoginCompletionService {

    private final GoogleIdentityService googleIdentityService;
    private final RefreshSessionService refreshSessionService;
    private final OAuthLoginHandoffService handoffService;

    public OAuthLoginCompletionService(
            GoogleIdentityService googleIdentityService,
            RefreshSessionService refreshSessionService,
            OAuthLoginHandoffService handoffService) {

        this.googleIdentityService = googleIdentityService;
        this.refreshSessionService = refreshSessionService;
        this.handoffService = handoffService;
    }

    /**
     * Completes Google authentication using FinTracker's own identity
     * and session system.
     *
     * All database writes participate in this transaction:
     * user/identity creation, refresh session creation, and handoff creation.
     */
    @Transactional
    public OAuthLoginCompletion completeLogin(OidcUser oidcUser) {

        if (oidcUser == null) {
            throw new IllegalArgumentException("Google identity is required.");
        }

        // Resolve an existing Google identity or create a new FinTracker user.
        AppUser user = googleIdentityService.resolveGoogleUser(oidcUser);

        // Create FinTracker's own persistent refresh-token session.
        LoginResult loginResult =
                refreshSessionService.createSessionForAuthenticatedUser(user);

        // Bind the one-time handoff code to that session's refresh token.
        String handoffCode = handoffService.createHandoff(
                user,
                loginResult.refreshToken());

        return new OAuthLoginCompletion(
                handoffCode,
                loginResult.refreshToken());
    }
}