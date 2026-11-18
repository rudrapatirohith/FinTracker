package com.fintracker.coreapi.identity.service;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fintracker.coreapi.identity.dto.response.LoginResponse;
import com.fintracker.coreapi.identity.entity.AppUser;
import com.fintracker.coreapi.identity.entity.UserSession;
import com.fintracker.coreapi.identity.entity.UserStatus;
import com.fintracker.coreapi.identity.repository.AppUserRepository;
import com.fintracker.coreapi.identity.repository.UserSessionRepository;
import com.fintracker.coreapi.identity.exception.InvalidCredentialsException;

@Service
public class LoginService {

    private final AppUserRepository appUserRepository;
    private final UserSessionRepository userSessionRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final RefreshTokenService refreshTokenService;
    private final Duration accessTokenTtl;
    private final Duration refreshTokenTtl;

    public LoginService(
        AppUserRepository appUserRepository,
        UserSessionRepository userSessionRepository,
        PasswordEncoder passwordEncoder,
        JwtTokenService jwtTokenService,
        RefreshTokenService refreshTokenService,
        @Value("${fintracker.security.jwt.access-token-ttl}") Duration accessTokenTtl,
        @Value("${fintracker.security.refresh-token.ttl}") Duration refreshTokenTtl
    ) {
        this.appUserRepository = appUserRepository;
        this.userSessionRepository = userSessionRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
        this.refreshTokenService = refreshTokenService;
        this.accessTokenTtl = accessTokenTtl;
        this.refreshTokenTtl = refreshTokenTtl;
    }

    /**
     * Authenticates the user and creates a persistent refresh-token session.
     */
    @Transactional
    public LoginResult login(String email, String password) {

        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);

        AppUser user = appUserRepository
            .findByEmailNormalized(normalizedEmail)
            .orElseThrow(InvalidCredentialsException::new);

        // OAuth-only accounts do not have a local password.
        if (user.getPasswordHash() == null
            || !passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new InvalidCredentialsException();
        }

        // Credentials are valid. Issue a short-lived access token.
        String accessToken = jwtTokenService.createAccessToken(user.getId());

        // Generate a separate, long-lived refresh token.
        String rawRefreshToken = refreshTokenService.generateToken();

        // Only the hash is persisted. The raw token is never stored.
        String refreshTokenHash =
            refreshTokenService.hashToken(rawRefreshToken);

        UUID tokenFamilyId = UUID.randomUUID();

        Instant expiresAt = Instant.now().plus(refreshTokenTtl);

        UserSession session = new UserSession(
            UUID.randomUUID(),
            user,
            refreshTokenHash,
            tokenFamilyId,
            expiresAt
        );

        userSessionRepository.save(session);

        LoginResponse response = new LoginResponse(
            accessToken,
            "Bearer",
            accessTokenTtl.toSeconds(),
            user.getId(),
            user.getEmail()
        );

        return new LoginResult(response, rawRefreshToken);
    }
}