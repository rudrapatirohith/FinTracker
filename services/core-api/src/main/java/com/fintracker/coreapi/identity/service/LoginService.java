package com.fintracker.coreapi.identity.service;

import java.time.Duration;
import java.util.Locale;

import com.fintracker.coreapi.identity.dto.request.LoginRequest;
import com.fintracker.coreapi.identity.dto.response.LoginResponse;
import com.fintracker.coreapi.identity.entity.AppUser;
import com.fintracker.coreapi.identity.entity.UserStatus;
import com.fintracker.coreapi.identity.exception.InvalidCredentialsException;
import com.fintracker.coreapi.identity.repository.AppUserRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Authenticates users using email and password.
 *
 * This service verifies credentials and issues an access token.
 * Refresh-token session management will be added in a later step.
 */
@Service
public class LoginService {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final long accessTokenTtlSeconds;

    public LoginService(
            AppUserRepository appUserRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenService jwtTokenService,
            @Value("${fintracker.security.jwt.access-token-ttl}")
            Duration accessTokenTtl) {

        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
        this.accessTokenTtlSeconds = accessTokenTtl.toSeconds();
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {

        // Normalize email consistently with the registration service.
        String normalizedEmail = request.email()
            .trim()
            .toLowerCase(Locale.ROOT);

        AppUser user = appUserRepository
            .findByEmailNormalized(normalizedEmail)
            .orElseThrow(InvalidCredentialsException::new);

        // OAuth-only accounts may not have a password hash, , so they cannot authenticate through password login.
        if (user.getPasswordHash() == null
                || !passwordEncoder.matches(   // never decrypt a stored password or compare plaintext passwords.
                    request.password(),
                    user.getPasswordHash()
                )) {

            throw new InvalidCredentialsException();
        }

        // Only active accounts may authenticate, Locked and disabled accounts cannot log in.
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new InvalidCredentialsException();
        }

        // Credentials are valid. Issue a short-lived access token.
        String accessToken = jwtTokenService.createAccessToken(user.getId());

        return new LoginResponse(
            accessToken,
            "Bearer",
            accessTokenTtlSeconds,
            user.getId(),
            user.getEmail()
        );
    }
}