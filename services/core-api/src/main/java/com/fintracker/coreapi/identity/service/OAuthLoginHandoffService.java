package com.fintracker.coreapi.identity.service;

import com.fintracker.coreapi.identity.entity.AppUser;
import com.fintracker.coreapi.identity.entity.OAuthLoginHandoff;
import com.fintracker.coreapi.identity.repository.OAuthLoginHandoffRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OAuthLoginHandoffService {

    private final OAuthLoginHandoffRepository handoffRepository;
    private final RefreshTokenService refreshTokenService;
    private final Duration handoffTtl;

    public OAuthLoginHandoffService(
            OAuthLoginHandoffRepository handoffRepository,
            RefreshTokenService refreshTokenService,
            @Value("${fintracker.security.oauth.handoff-ttl}") Duration handoffTtl) {

        this.handoffRepository = handoffRepository;
        this.refreshTokenService = refreshTokenService;
        this.handoffTtl = handoffTtl;
    }

    /**
     * Creates a short-lived, single-use code for completing Google login.
     *
     * Only the SHA-256 hash of the code is stored in the database.
     * The raw code is returned once and sent to the frontend through
     * the OAuth redirect.
     *
     * The handoff is bound to the refresh token issued for this login.
     */
    @Transactional
    public String createHandoff(AppUser user, String rawRefreshToken) {

        if (user == null || rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new IllegalArgumentException("An authenticated user and refresh token are required.");
        }

        Instant now = Instant.now();

        // Generate a cryptographically random, URL-safe code.
        String rawCode = refreshTokenService.generateToken();

        // Never store the raw handoff code or refresh token.
        String codeHash = refreshTokenService.hashToken(rawCode);
        String refreshTokenHash = refreshTokenService.hashToken(rawRefreshToken);

        OAuthLoginHandoff handoff =
                new OAuthLoginHandoff(UUID.randomUUID(), codeHash, user, refreshTokenHash, now, now.plus(handoffTtl));

        handoffRepository.save(handoff);

        return rawCode;
    }
}
