/**
 * Single source of truth for resolving the current application's user ID from Spring Security.
 * Financial services call this component rather than accepting user IDs from request bodies or trusting arbitrary principal types.
 * It requires an authenticated Jwt principal and parses its subject as a UUID; missing, unauthenticated, non-JWT, blank, or malformed identities fail closed with InvalidAuthenticatedUserException.
 */
package com.fintracker.coreapi.identity.service;

import com.fintracker.coreapi.identity.exception.InvalidAuthenticatedUserException;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public class AuthenticatedUserProvider {

    public UUID getRequiredUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new InvalidAuthenticatedUserException();
        }

        if (!(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw new InvalidAuthenticatedUserException();
        }

        String subject = jwt.getSubject();

        if (subject == null || subject.isBlank()) {
            throw new InvalidAuthenticatedUserException();
        }

        try {
            return UUID.fromString(subject);
        } catch (IllegalArgumentException exception) {
            throw new InvalidAuthenticatedUserException();
        }
    }
}
