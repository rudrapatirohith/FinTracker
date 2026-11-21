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

    @AfterEach
    /**
     * Clears Spring Security's thread-local context after the test to prevent authentication leaking into another test.
     */
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    /**
     * Verifies that returns user id from authenticated jwt subject for the authenticated JWT principal.
     */
    void returnsUserIdFromAuthenticatedJwtSubject() {
        UUID expectedUserId = UUID.randomUUID();

        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .subject(expectedUserId.toString())
                .build();

      /**
       * Installs a test authentication whose JWT subject identifies the supplied user, allowing the service to resolve tenant ownership exactly as it does in a request.
       */
        authenticate(jwt);

        assertEquals(expectedUserId, provider.getRequiredUserId());
    }

    @Test
    /**
     * Verifies that rejects missing authentication and that invalid identity state is rejected with the expected exception.
     */
    void rejectsMissingAuthentication() {
        SecurityContextHolder.clearContext();

        assertThrows(InvalidAuthenticatedUserException.class, provider::getRequiredUserId);
    }

    @Test
    /**
     * Verifies that rejects non jwt principal and that invalid identity state is rejected with the expected exception.
     */
    void rejectsNonJwtPrincipal() {
      /**
       * Installs a test authentication whose JWT subject identifies the supplied user, allowing the service to resolve tenant ownership exactly as it does in a request.
       */
        authenticate("not-a-jwt");

        assertThrows(InvalidAuthenticatedUserException.class, provider::getRequiredUserId);
    }

    @Test
    /**
     * Verifies that rejects missing jwt subject and that invalid identity state is rejected with the expected exception.
     */
    void rejectsMissingJwtSubject() {
        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .claim("token_use", "access")
                .build();

      /**
       * Installs a test authentication whose JWT subject identifies the supplied user, allowing the service to resolve tenant ownership exactly as it does in a request.
       */
        authenticate(jwt);

        assertThrows(InvalidAuthenticatedUserException.class, provider::getRequiredUserId);
    }

    @Test
    /**
     * Verifies that rejects malformed jwt subject and that invalid identity state is rejected with the expected exception.
     */
    void rejectsMalformedJwtSubject() {
        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .subject("not-a-uuid")
                .build();

      /**
       * Installs a test authentication whose JWT subject identifies the supplied user, allowing the service to resolve tenant ownership exactly as it does in a request.
       */
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
