package com.fintracker.coreapi.identity.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fintracker.coreapi.identity.dto.response.LoginResponse;
import com.fintracker.coreapi.identity.entity.AppUser;
import com.fintracker.coreapi.identity.entity.OAuthLoginHandoff;
import com.fintracker.coreapi.identity.entity.UserSession;
import com.fintracker.coreapi.identity.entity.UserStatus;
import com.fintracker.coreapi.identity.exception.InvalidOAuthHandoffException;
import com.fintracker.coreapi.identity.repository.OAuthLoginHandoffRepository;
import com.fintracker.coreapi.identity.repository.UserSessionRepository;

@Service
public class OAuthLoginExchangeService {

    private final OAuthLoginHandoffRepository handoffRepository;
    private final UserSessionRepository userSessionRepository;
    private final RefreshTokenService refreshTokenService;
    private final JwtTokenService jwtTokenService;
    private final Duration accessTokenTtl;

    public OAuthLoginExchangeService(
            OAuthLoginHandoffRepository handoffRepository,
            UserSessionRepository userSessionRepository,
            RefreshTokenService refreshTokenService,
            JwtTokenService jwtTokenService,
            @Value("${fintracker.security.jwt.access-token-ttl}") Duration accessTokenTtl) {

        this.handoffRepository = handoffRepository;
        this.userSessionRepository = userSessionRepository;
        this.refreshTokenService = refreshTokenService;
        this.jwtTokenService = jwtTokenService;
        this.accessTokenTtl = accessTokenTtl;
    }

    /**
     * Exchanges a one-time OAuth handoff code for an application access token.
     *
     * The handoff code must be accompanied by the refresh token issued
     * during the same OAuth login.
     */
    @Transactional
    public LoginResponse exchange(String rawCode, String rawRefreshToken) {

        if (rawCode == null || rawCode.isBlank()
                || rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new InvalidOAuthHandoffException();
        }

        Instant now = Instant.now();

        String codeHash = refreshTokenService.hashToken(rawCode);
        String refreshTokenHash = refreshTokenService.hashToken(rawRefreshToken);

        // Lock the handoff row so two simultaneous requests cannot consume it.
        OAuthLoginHandoff handoff = handoffRepository
                .findByCodeHashForUpdate(codeHash)
                .orElseThrow(InvalidOAuthHandoffException::new);

        if (handoff.isConsumed() || handoff.isExpired(now)) {
            throw new InvalidOAuthHandoffException();
        }

        // Lock and validate the refresh-token session bound to this handoff.
        UserSession session = userSessionRepository
                .findByRefreshTokenHashForUpdate(refreshTokenHash)
                .orElseThrow(InvalidOAuthHandoffException::new);

        if (!session.getExpiresAt().isAfter(now)
                || session.getRevokedAt() != null) {
            throw new InvalidOAuthHandoffException();
        }

        AppUser user = handoff.getUser();

        if (user == null
                || user.getStatus() != UserStatus.ACTIVE
                || session.getUser() == null
                || !session.getUser().getId().equals(user.getId())) {
            throw new InvalidOAuthHandoffException();
        }

        // Compare the stored hash with the supplied cookie hash.
        if (!MessageDigest.isEqual(
                handoff.getRefreshTokenHash().getBytes(StandardCharsets.US_ASCII),
                refreshTokenHash.getBytes(StandardCharsets.US_ASCII))) {
            throw new InvalidOAuthHandoffException();
        }

        // Mark the handoff as consumed. The row lock makes this single-use.
        handoff.consume(now);

        String accessToken = jwtTokenService.createAccessToken(user.getId());

        return new LoginResponse(
                accessToken,
                "Bearer",
                accessTokenTtl.toSeconds(),
                user.getId(),
                user.getEmail());
    }
}