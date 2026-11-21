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
 * Focused unit tests for JWT-to-application-user resolution.
 * Each test covers one security boundary: valid UUID subject, absent authentication, non-JWT principal, missing subject, or malformed UUID.
 * The security context is cleared after every test so authentication state cannot leak between test cases.
 */
class AuthenticatedUserProviderTests {

    private final AuthenticatedUserProvider provider = new AuthenticatedUserProvider();

/**
     * Clears Spring Security's thread-local context after the test to prevent authentication leaking into another test.
     */
    @AfterEach

    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

/**
     * Verifies that returns user id from authenticated jwt subject for the authenticated JWT principal.
     */
    @Test

    void returnsUserIdFromAuthenticatedJwtSubject() {
        UUID expectedUserId = UUID.randomUUID();

        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .subject(expectedUserId.toString())
                .build();

        authenticate(jwt);

        assertEquals(expectedUserId, provider.getRequiredUserId());
    }

/**
     * Verifies that rejects missing authentication and that invalid identity state is rejected with the expected exception.
     */
    @Test

    void rejectsMissingAuthentication() {
        SecurityContextHolder.clearContext();

        assertThrows(InvalidAuthenticatedUserException.class, provider::getRequiredUserId);
    }

/**
     * Verifies that rejects non jwt principal and that invalid identity state is rejected with the expected exception.
     */
    @Test

    void rejectsNonJwtPrincipal() {

        authenticate("not-a-jwt");

        assertThrows(InvalidAuthenticatedUserException.class, provider::getRequiredUserId);
    }

/**
     * Verifies that rejects missing jwt subject and that invalid identity state is rejected with the expected exception.
     */
    @Test

    void rejectsMissingJwtSubject() {
        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .claim("token_use", "access")
                .build();

        authenticate(jwt);

        assertThrows(InvalidAuthenticatedUserException.class, provider::getRequiredUserId);
    }

/**
     * Verifies that rejects malformed jwt subject and that invalid identity state is rejected with the expected exception.
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
