package com.fintracker.coreapi.identity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fintracker.coreapi.identity.exception.InvalidAuthenticatedUserException;
import com.fintracker.coreapi.identity.service.AuthenticatedUserProvider;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

/**
     * Verifies that a non-UUID JWT subject fails closed with InvalidAuthenticatedUserException.
     */
    @Test

    void rejectsMalformedJwtSubject() {
        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .subject("not-a-uuid")
                .build();

        authenticate(jwt);

        assertThrows(InvalidAuthenticatedUserException.class, provider::getRequiredUserId);
    }

    /**
     * Installs a test authentication whose JWT subject identifies the supplied user, allowing the service to resolve tenant ownership exactly as it does in a request.
     */
    private void authenticate(Object principal) {
        TestingAuthenticationToken authentication = new TestingAuthenticationToken(principal, null, "ROLE_USER");

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
