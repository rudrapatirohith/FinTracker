package com.fintracker.coreapi.identity.service;

import com.fintracker.coreapi.identity.exception.InvalidAuthenticatedUserException;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
/**
 * Centralizes conversion of the authenticated JWT subject into the application's user UUID. It rejects missing, unauthenticated, non-JWT, blank, and malformed principals so services fail closed.
 */
public class AuthenticatedUserProvider {

    /**
     * Requires an authenticated JWT principal and parses its subject as the application-user UUID; missing, non-JWT, blank, or malformed identities fail closed.
     */
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
