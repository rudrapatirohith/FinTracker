
package com.fintracker.coreapi.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintracker.coreapi.identity.dto.request.LoginRequest;
import com.fintracker.coreapi.identity.dto.request.RegisterRequest;
import com.fintracker.coreapi.identity.entity.AppUser;
import com.fintracker.coreapi.identity.entity.UserSession;
import com.fintracker.coreapi.identity.exception.InvalidRefreshTokenException;
import com.fintracker.coreapi.identity.exception.RefreshTokenReuseException;
import com.fintracker.coreapi.identity.repository.AppUserRepository;
import com.fintracker.coreapi.identity.repository.UserSessionRepository;
import com.fintracker.coreapi.identity.service.LoginResult;
import com.fintracker.coreapi.identity.service.RefreshSessionService;
import com.fintracker.coreapi.identity.service.RefreshTokenService;
import com.fintracker.coreapi.identity.service.RegistrationService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import jakarta.servlet.http.Cookie;



@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RefreshSessionIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RegistrationService registrationService;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private UserSessionRepository userSessionRepository;

    @Autowired
    private RefreshSessionService refreshSessionService;

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final String EMAIL = "refresh.test@example.com";
    private static final String PASSWORD = "StrongPassword123!";

    /**
     * Remove dependent records before their parent user records.
     *
     * This keeps every test independent and respects foreign-key constraints.
     */
    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.update("DELETE FROM user_sessions");
        jdbcTemplate.update("DELETE FROM oauth_identities");
        jdbcTemplate.update("DELETE FROM user_profiles");
        jdbcTemplate.update("DELETE FROM app_users");
    }

    /**
     * Register a real application user for the integration tests.
     */
    private AppUser createTestUser() {

        registrationService.register(
            new RegisterRequest(
                EMAIL,
                PASSWORD,
                "Refresh Test User"
            )
        );

        return appUserRepository
            .findByEmailNormalized(EMAIL.toLowerCase())
            .orElseThrow();
    }

    /**
     * Log in through the HTTP endpoint and extract the refresh token
     * from the Set-Cookie response header.
     *
     * This tests the actual login cookie behavior instead of manually
     * creating a refresh token unrelated to the login flow.
     */
    private String loginAndGetRefreshToken() throws Exception {

        LoginRequest request = new LoginRequest(
            EMAIL,
            PASSWORD
        );

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andReturn();

        String setCookie = result.getResponse()
            .getHeader("Set-Cookie");

        assertThat(setCookie).isNotNull();

        // The first section of Set-Cookie contains name=value.
        String cookiePair = setCookie.split(";")[0];

        int separatorIndex = cookiePair.indexOf('=');

        assertThat(separatorIndex).isGreaterThan(0);

        return cookiePair.substring(separatorIndex + 1);
    }

    @Test
    void shouldRotateValidRefreshToken() throws Exception {

        createTestUser();

        // Login creates the initial persistent session.
        String originalToken = loginAndGetRefreshToken();

        assertThat(userSessionRepository.count()).isEqualTo(1);

        // Exchange the original refresh token for a new token pair.
        LoginResult result =
            refreshSessionService.refresh(originalToken);

        String replacementToken = result.refreshToken();

        // The replacement token must be different from the original token.
        assertThat(replacementToken)
            .isNotBlank()
            .isNotEqualTo(originalToken);

        // A new access JWT must also have been created.
        assertThat(result.response().accessToken()).isNotBlank();

        // Look up both sessions using their stored hashes.
        UserSession oldSession = userSessionRepository
            .findByRefreshTokenHash(
                refreshTokenService.hashToken(originalToken)
            )
            .orElseThrow();

        UserSession newSession = userSessionRepository
            .findByRefreshTokenHash(
                refreshTokenService.hashToken(replacementToken)
            )
            .orElseThrow();

        // The original session must now be revoked because it was rotated.
        assertThat(oldSession.getRevokedAt()).isNotNull();
        assertThat(oldSession.getRevocationReason()).isEqualTo("ROTATED");

        // The replacement session must remain active.
        assertThat(newSession.getRevokedAt()).isNull();

        // Both sessions must belong to the same token family.
        assertThat(newSession.getTokenFamilyId())
            .isEqualTo(oldSession.getTokenFamilyId());

        assertThat(userSessionRepository.count()).isEqualTo(2);
    }

    @Test
    void shouldRevokeActiveFamilySessionsWhenRotatedTokenIsReused()
        throws Exception {

        createTestUser();

        // Initial login creates the original session.
        String originalToken = loginAndGetRefreshToken();

        // First use is legitimate and creates a replacement session.
        LoginResult result =
            refreshSessionService.refresh(originalToken);

        String replacementToken = result.refreshToken();

        // Present the original, already-rotated token again.
        assertThatThrownBy(
            () -> refreshSessionService.refresh(originalToken)
        )
            .isInstanceOf(RefreshTokenReuseException.class);

        /*
         * The original session retains ROTATED as its reason.
         * The active replacement session is revoked with REUSE_DETECTED.
         */
        UserSession oldSession = userSessionRepository
            .findByRefreshTokenHash(
                refreshTokenService.hashToken(originalToken)
            )
            .orElseThrow();

        UserSession replacementSession = userSessionRepository
            .findByRefreshTokenHash(
                refreshTokenService.hashToken(replacementToken)
            )
            .orElseThrow();

        assertThat(oldSession.getRevokedAt()).isNotNull();
        assertThat(oldSession.getRevocationReason()).isEqualTo("ROTATED");

        assertThat(replacementSession.getRevokedAt()).isNotNull();
        assertThat(replacementSession.getRevocationReason())
            .isEqualTo("REUSE_DETECTED");

        /*
         * The replacement token has now been revoked.
         * Presenting it must be rejected as an invalid token, not classified
         * as another reuse event.
         */
        assertThatThrownBy(
            () -> refreshSessionService.refresh(replacementToken)
        )
            .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void shouldRejectUnknownTokenWithoutRevokingValidSession()
        throws Exception {

        createTestUser();

        String validToken = loginAndGetRefreshToken();

        // This token was never generated by our application.
        assertThatThrownBy(
            () -> refreshSessionService.refresh("unknown-random-token")
        )
            .isInstanceOf(InvalidRefreshTokenException.class);

        // An unknown token must not affect a real user's active session.
        UserSession validSession = userSessionRepository
            .findByRefreshTokenHash(
                refreshTokenService.hashToken(validToken)
            )
            .orElseThrow();

        assertThat(validSession.getRevokedAt()).isNull();
    }

    @Test
    void shouldRejectAndPersistRevocationOfExpiredToken() {

        AppUser user = createTestUser();

        // Generate a valid random token but give its session an expired date.
        String rawToken = refreshTokenService.generateToken();

        UserSession expiredSession = new UserSession(
            UUID.randomUUID(),
            user,
            refreshTokenService.hashToken(rawToken),
            UUID.randomUUID(),
            Instant.now().minusSeconds(60)
        );

        userSessionRepository.saveAndFlush(expiredSession);

        // The service must reject the expired token.
        assertThatThrownBy(
            () -> refreshSessionService.refresh(rawToken)
        )
            .isInstanceOf(InvalidRefreshTokenException.class);

        // Verify that the revocation was committed despite the exception.
        UserSession savedSession = userSessionRepository
            .findByRefreshTokenHash(
                refreshTokenService.hashToken(rawToken)
            )
            .orElseThrow();

        assertThat(savedSession.getRevokedAt()).isNotNull();
        assertThat(savedSession.getRevocationReason()).isEqualTo("EXPIRED");
    }


    @Test
    void shouldRefreshThroughHttpEndpointAndRotateCookie() throws Exception {

        createTestUser();

        // Login through HTTP and obtain the original refresh token.
        String originalToken = loginAndGetRefreshToken();

        Cookie originalCookie =
            new Cookie("fintracker_refresh", originalToken);

        // Submit the refresh request with its cookie and CSRF token.
        MvcResult result = mockMvc.perform(
                post("/api/v1/auth/refresh")
                    .with(csrf())
                    .cookie(originalCookie)
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").isNotEmpty())
            .andExpect(jsonPath("$.tokenType").value("Bearer"))
            .andExpect(jsonPath("$.refreshToken").doesNotExist())
            .andReturn();

        // The response must provide a replacement refresh cookie.
        String setCookie = result.getResponse()
            .getHeader("Set-Cookie");

        assertThat(setCookie).isNotNull();
        assertThat(setCookie).contains("HttpOnly");
        assertThat(setCookie).contains("Path=/api/v1/auth");

        // Extract the replacement cookie value.
        String cookiePair = setCookie.split(";")[0];
        String replacementToken =
            cookiePair.substring(cookiePair.indexOf('=') + 1);

        assertThat(replacementToken)
            .isNotBlank()
            .isNotEqualTo(originalToken);

        // Verify that the old token is revoked.
        UserSession oldSession = userSessionRepository
            .findByRefreshTokenHash(
                refreshTokenService.hashToken(originalToken)
            )
            .orElseThrow();

        assertThat(oldSession.getRevocationReason())
            .isEqualTo("ROTATED");

        // Verify that the replacement token has an active session.
        UserSession newSession = userSessionRepository
            .findByRefreshTokenHash(
                refreshTokenService.hashToken(replacementToken)
            )
            .orElseThrow();

        assertThat(newSession.getRevokedAt()).isNull();
    }



    @Test
    void shouldRejectRefreshWithoutCsrfToken() throws Exception {

        createTestUser();

        String refreshToken = loginAndGetRefreshToken();

        mockMvc.perform(
                post("/api/v1/auth/refresh")
                    .cookie(
                        new Cookie("fintracker_refresh", refreshToken)
                    )
            )
            .andExpect(status().isForbidden());

        // A rejected request must not rotate or revoke the valid session.
        UserSession session = userSessionRepository
            .findByRefreshTokenHash(
                refreshTokenService.hashToken(refreshToken)
            )
            .orElseThrow();

        assertThat(session.getRevokedAt()).isNull();
    }


    @Test
    void shouldClearCookieWhenHttpRefreshTokenIsInvalid() throws Exception {

        createTestUser();

        MvcResult result = mockMvc.perform(
                post("/api/v1/auth/refresh")
                    .with(csrf())
                    .cookie(
                        new Cookie(
                            "fintracker_refresh",
                            "unknown-random-token"
                        )
                    )
            )
            .andExpect(status().isUnauthorized())
            .andReturn();

        String setCookie = result.getResponse()
            .getHeader("Set-Cookie");

        assertThat(setCookie).isNotNull();
        assertThat(setCookie).contains("Max-Age=0");
        assertThat(setCookie).contains("HttpOnly");
    }

}
