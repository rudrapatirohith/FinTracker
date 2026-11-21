package com.fintracker.coreapi.identity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import com.fintracker.coreapi.identity.exception.InvalidAuthenticatedUserException;
import com.fintracker.coreapi.identity.service.AuthenticatedUserProvider;

class AuthenticatedUserProviderTests {

        private final AuthenticatedUserProvider provider = new AuthenticatedUserProvider();

        /**
         * Clears the security context after each test so authentication cannot leak between scenarios.
         */
        @AfterEach
        void clearSecurityContext() {
                SecurityContextHolder.clearContext();
        }

        /**
         * Verifies that a valid authenticated JWT subject is parsed into the matching application-user UUID.
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
         * Verifies that user resolution fails when no authenticated principal is present.
         */
        @Test
        void rejectsMissingAuthentication() {
                SecurityContextHolder.clearContext();

                assertThrows(
                                InvalidAuthenticatedUserException.class,
                                provider::getRequiredUserId);
        }

        /**
         * Verifies that an authenticated principal that is not a JWT is rejected rather than treated as a user identity.
         */
        @Test
        void rejectsNonJwtPrincipal() {
                authenticate("not-a-jwt");

                assertThrows(
                                InvalidAuthenticatedUserException.class,
                                provider::getRequiredUserId);
        }

        /**
         * Verifies that a JWT without a subject cannot be mapped to an application-user UUID.
         */
        @Test
        void rejectsMissingJwtSubject() {
                Jwt jwt = Jwt.withTokenValue("test-token")
                                .header("alg", "none")
                                .claim("token_use", "access")
                                .build();

                authenticate(jwt);

                assertThrows(
                                InvalidAuthenticatedUserException.class,
                                provider::getRequiredUserId);
        }

        /**
         * Verifies that a subject that is not a UUID is rejected with InvalidAuthenticatedUserException.
         */
        @Test
        void rejectsMalformedJwtSubject() {
                Jwt jwt = Jwt.withTokenValue("test-token")
                                .header("alg", "none")
                                .subject("not-a-uuid")
                                .build();

                authenticate(jwt);

                assertThrows(
                                InvalidAuthenticatedUserException.class,
                                provider::getRequiredUserId);
        }

        /**
         * Installs the supplied principal as an authenticated test user in Spring Security's context.
         */
        private void authenticate(Object principal) {
                TestingAuthenticationToken authentication = new TestingAuthenticationToken(
                                principal,
                                null,
                                "ROLE_USER");

                SecurityContextHolder.getContext()
                                .setAuthentication(authentication);
        }
}