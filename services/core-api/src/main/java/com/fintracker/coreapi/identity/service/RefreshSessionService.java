package com.fintracker.coreapi.identity.service;

import com.fintracker.coreapi.identity.dto.response.LoginResponse;
import com.fintracker.coreapi.identity.entity.AppUser;
import com.fintracker.coreapi.identity.entity.UserSession;
import com.fintracker.coreapi.identity.entity.UserStatus;
import com.fintracker.coreapi.identity.exception.InvalidRefreshTokenException;
import com.fintracker.coreapi.identity.exception.RefreshTokenReuseException;
import com.fintracker.coreapi.identity.repository.UserSessionRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RefreshSessionService {

    // Reasons recorded in the database when a refresh session is revoked.
    private static final String REASON_ROTATED = "ROTATED";
    private static final String REASON_REUSE_DETECTED = "REUSE_DETECTED";
    private static final String REASON_EXPIRED = "EXPIRED";
    private static final String REASON_USER_INACTIVE = "USER_INACTIVE";

    private final UserSessionRepository userSessionRepository;
    private final RefreshTokenService refreshTokenService;
    private final JwtTokenService jwtTokenService;
    private final Duration accessTokenTtl;
    private final Duration refreshTokenTtl;

    public RefreshSessionService(
            UserSessionRepository userSessionRepository,
            RefreshTokenService refreshTokenService,
            JwtTokenService jwtTokenService,
            @Value("${fintracker.security.jwt.access-token-ttl}") Duration accessTokenTtl,
            @Value("${fintracker.security.refresh-token.ttl}") Duration refreshTokenTtl) {
        this.userSessionRepository = userSessionRepository;
        this.refreshTokenService = refreshTokenService;
        this.jwtTokenService = jwtTokenService;
        this.accessTokenTtl = accessTokenTtl;
        this.refreshTokenTtl = refreshTokenTtl;
    }

    /**
     * Validates a refresh token and exchanges it for a new token pair.
     *
     * The replacement refresh token is returned internally so that
     * the controller can send it through an HttpOnly cookie.
     *
     * Only the SHA-256 hash of the refresh token is stored in PostgreSQL.
     *
     * noRollbackFor is important because we sometimes need to persist
     * session revocation even though this method returns an exception.
     */
    @Transactional(noRollbackFor = {RefreshTokenReuseException.class, InvalidRefreshTokenException.class})
    public LoginResult refresh(String rawRefreshToken) {

        // Reject missing or blank tokens before accessing the database.
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new InvalidRefreshTokenException();
        }

        // Hash the presented token because raw refresh tokens are never stored.
        String presentedHash = refreshTokenService.hashToken(rawRefreshToken);

        /*
         * Lock the matching session row until this transaction completes.
         *
         * This prevents two requests from rotating the same active token
         * simultaneously.
         */
        UserSession currentSession = userSessionRepository
                .findByRefreshTokenHashForUpdate(presentedHash)
                .orElseThrow(InvalidRefreshTokenException::new);

        Instant now = Instant.now();

        /*
         * Only a token previously revoked because of ROTATED is considered
         * a reuse attempt.
         *
         * Expired tokens and tokens revoked for other reasons are rejected
         * without incorrectly classifying them as token theft.
         */
        if (currentSession.getRevokedAt() != null) {

            if (REASON_ROTATED.equals(currentSession.getRevocationReason())) {

                // Revoke all remaining active sessions in this token family.
                userSessionRepository.revokeActiveByTokenFamilyId(
                        currentSession.getTokenFamilyId(), now, REASON_REUSE_DETECTED);

                throw new RefreshTokenReuseException();
            }

            throw new InvalidRefreshTokenException();
        }

        // Expired sessions cannot be exchanged for new tokens.
        if (!currentSession.getExpiresAt().isAfter(now)) {

            currentSession.revoke(now, REASON_EXPIRED);

            throw new InvalidRefreshTokenException();
        }

        AppUser user = currentSession.getUser();

        // Do not issue new tokens to locked or disabled accounts.
        if (user.getStatus() != UserStatus.ACTIVE) {

            currentSession.revoke(now, REASON_USER_INACTIVE);

            throw new InvalidRefreshTokenException();
        }

        // Generate a new cryptographically random refresh token.
        String replacementRawToken = refreshTokenService.generateToken();

        // Persist only the SHA-256 hash of the replacement token.
        String replacementHash = refreshTokenService.hashToken(replacementRawToken);

        /*
         * A token family connects the original login session to all
         * replacement sessions created through token rotation.
         */
        UserSession replacementSession = new UserSession(
                UUID.randomUUID(), user, replacementHash, currentSession.getTokenFamilyId(), now.plus(refreshTokenTtl));

        /*
         * IMPORTANT HIBERNATE FIX:
         *
         * IDs are assigned before saving in this project. Spring Data JPA
         * can therefore use merge() instead of persist().
         *
         * saveAndFlush() returns the managed entity instance and flushes
         * the INSERT before we create the relationship to the old session.
         *
         * Always use the returned instance below, not replacementSession.
         */
        UserSession persistedReplacementSession = userSessionRepository.saveAndFlush(replacementSession);

        /*
         * Mark the old session as rotated and link it to the managed
         * replacement entity.
         *
         * This avoids Hibernate's TransientObjectException.
         */
        currentSession.markRotated(now, persistedReplacementSession);

        // Generate a fresh short-lived access JWT.
        String accessToken = jwtTokenService.createAccessToken(user.getId());

        LoginResponse response =
                new LoginResponse(accessToken, "Bearer", accessTokenTtl.toSeconds(), user.getId(), user.getEmail());

        // The controller will place this raw token in an HttpOnly cookie.
        return new LoginResult(response, replacementRawToken);
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        // A missing cookie is not an error during logout.
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            return;
        }

        String tokenHash = refreshTokenService.hashToken(rawRefreshToken);

        // Lock the matching session so logout cannot race with token rotation.
        UserSession session =
                userSessionRepository.findByRefreshTokenHashForUpdate(tokenHash).orElse(null);

        // Unknown tokens cannot identify a session family.
        if (session == null) {
            return;
        }

        // Revoke every active session in this device's rotation family.
        userSessionRepository.revokeActiveByTokenFamilyId(session.getTokenFamilyId(), Instant.now(), "LOGOUT");
    }

    /**
     * Creates a new FinTracker session for a user who has already
     * been authenticated by a trusted authentication mechanism.
     *
     * The caller is responsible for verifying the user's identity.
     * This service creates our application's own JWT and refresh token.
     */
    @Transactional
    public LoginResult createSessionForAuthenticatedUser(AppUser user) {

        if (user == null || user.getStatus() != UserStatus.ACTIVE) {
            throw new IllegalArgumentException("An active authenticated user is required.");
        }

        Instant now = Instant.now();

        // Generate a cryptographically random refresh token.
        String rawRefreshToken = refreshTokenService.generateToken();

        // Store only its SHA-256 hash.
        String refreshTokenHash = refreshTokenService.hashToken(rawRefreshToken);

        // A new login creates a new independent token family.
        UUID tokenFamilyId = UUID.randomUUID();

        UserSession session =
                new UserSession(UUID.randomUUID(), user, refreshTokenHash, tokenFamilyId, now.plus(refreshTokenTtl));

        userSessionRepository.saveAndFlush(session);

        // Issue our own FinTracker access JWT.
        String accessToken = jwtTokenService.createAccessToken(user.getId());

        LoginResponse response =
                new LoginResponse(accessToken, "Bearer", accessTokenTtl.toSeconds(), user.getId(), user.getEmail());

        return new LoginResult(response, rawRefreshToken);
    }
}
