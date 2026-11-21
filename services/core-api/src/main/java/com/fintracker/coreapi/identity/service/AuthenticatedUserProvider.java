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

      /**
       * Performs the if step for this component.
       */
        if (authentication == null || !authentication.isAuthenticated()) {
            /**
             * Performs the invalid authenticated user exception step for this component.
             */
            throw new InvalidAuthenticatedUserException();
        }

      /**
       * Performs the if step for this component.
       */
        if (!(authentication.getPrincipal() instanceof Jwt jwt)) {
            /**
             * Performs the invalid authenticated user exception step for this component.
             */
            throw new InvalidAuthenticatedUserException();
        }

        String subject = jwt.getSubject();

      /**
       * Performs the if step for this component.
       */
        if (subject == null || subject.isBlank()) {
            /**
             * Performs the invalid authenticated user exception step for this component.
             */
            throw new InvalidAuthenticatedUserException();
        }

        try {
            return UUID.fromString(subject);
        } catch (IllegalArgumentException exception) {
            /**
             * Performs the invalid authenticated user exception step for this component.
             */
            throw new InvalidAuthenticatedUserException();
        }
    }
}
