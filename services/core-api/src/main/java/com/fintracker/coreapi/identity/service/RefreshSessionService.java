package com.fintracker.coreapi.identity.service;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fintracker.coreapi.identity.dto.response.LoginResponse;
import com.fintracker.coreapi.identity.entity.AppUser;
import com.fintracker.coreapi.identity.entity.UserSession;
import com.fintracker.coreapi.identity.entity.UserStatus;
import com.fintracker.coreapi.identity.exception.InvalidRefreshTokenException;
import com.fintracker.coreapi.identity.exception.RefreshTokenReuseException;
import com.fintracker.coreapi.identity.repository.UserSessionRepository;

@Service
public class RefreshSessionService {

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
        @Value("${fintracker.security.refresh-token.ttl}") Duration refreshTokenTtl
    ) {
        this.userSessionRepository = userSessionRepository;
        this.refreshTokenService = refreshTokenService;
        this.jwtTokenService = jwtTokenService;
        this.accessTokenTtl = accessTokenTtl;
        this.refreshTokenTtl = refreshTokenTtl;
    }

    /**
     * Validates a refresh token and rotates it into a new session.
     *
     * The raw replacement token is returned internally so the controller
     * can issue it as an HttpOnly cookie. Only its hash is stored.
     *
     * noRollbackFor is important: when reuse is detected, the family
     * revocation must commit even though the method reports an exception.
     */
    @Transactional(noRollbackFor = {
        RefreshTokenReuseException.class,
        InvalidRefreshTokenException.class
    })
    public LoginResult refresh(String rawRefreshToken) {

        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new InvalidRefreshTokenException();
        }

        String presentedHash =
            refreshTokenService.hashToken(rawRefreshToken);

        UserSession currentSession = userSessionRepository
            .findByRefreshTokenHashForUpdate(presentedHash)
            .orElseThrow(InvalidRefreshTokenException::new);

        Instant now = Instant.now();

        // A known token that was already revoked is a reuse attempt.
        if (currentSession.getRevokedAt() != null) {
            userSessionRepository.revokeActiveByTokenFamilyId(
                currentSession.getTokenFamilyId(),
                now,
                REASON_REUSE_DETECTED
            );

            throw new RefreshTokenReuseException();
        }

        // Expired sessions cannot be refreshed.
        if (!currentSession.getExpiresAt().isAfter(now)) {
            currentSession.revoke(now, REASON_EXPIRED);
            throw new InvalidRefreshTokenException();
        }

        AppUser user = currentSession.getUser();

        // Do not issue tokens for locked or disabled accounts.
        if (user.getStatus() != UserStatus.ACTIVE) {
            currentSession.revoke(now, REASON_USER_INACTIVE);
            throw new InvalidRefreshTokenException();
        }

        // Generate a completely new refresh token.
        String replacementRawToken =
            refreshTokenService.generateToken();

        String replacementHash =
            refreshTokenService.hashToken(replacementRawToken);

        // Keep the replacement in the same token family.
        UserSession replacementSession = new UserSession(
            UUID.randomUUID(),
            user,
            replacementHash,
            currentSession.getTokenFamilyId(),
            now.plus(refreshTokenTtl)
        );

        // Persist the replacement before linking the old session to it.
        userSessionRepository.save(replacementSession);

        currentSession.markRotated(now, replacementSession);

        // Create a fresh short-lived access token.
        String accessToken =
            jwtTokenService.createAccessToken(user.getId());

        LoginResponse response = new LoginResponse(
            accessToken,
            "Bearer",
            accessTokenTtl.toSeconds(),
            user.getId(),
            user.getEmail()
        );

        return new LoginResult(response, replacementRawToken);
    }
}