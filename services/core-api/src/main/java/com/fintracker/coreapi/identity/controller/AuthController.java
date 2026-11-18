package com.fintracker.coreapi.identity.controller;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fintracker.coreapi.identity.dto.request.LoginRequest;
import com.fintracker.coreapi.identity.dto.request.OAuthExchangeRequest;
import com.fintracker.coreapi.identity.dto.request.RegisterRequest;
import com.fintracker.coreapi.identity.dto.response.LoginResponse;
import com.fintracker.coreapi.identity.dto.response.RegisterResponse;
import com.fintracker.coreapi.identity.exception.InvalidRefreshTokenException;
import com.fintracker.coreapi.identity.exception.RefreshTokenReuseException;
import com.fintracker.coreapi.identity.service.LoginResult;
import com.fintracker.coreapi.identity.service.LoginService;
import com.fintracker.coreapi.identity.service.OAuthLoginExchangeService;
import com.fintracker.coreapi.identity.service.RefreshSessionService;
import com.fintracker.coreapi.identity.service.RegistrationService;

import jakarta.validation.Valid;

/**
 * Authentication-related REST endpoints.
 */
@RestController // Marks this class as a REST controller that handles HTTP requests. Combination of @Contoller and @Requestbody
@RequestMapping("/api/v1/auth")
@Validated
public class AuthController {

    private final RegistrationService registrationService;
    private final LoginService loginService;
    private final RefreshSessionService refreshSessionService;
    private final OAuthLoginExchangeService oauthLoginExchangeService;

    @Value("${fintracker.security.refresh-token.cookie-name}")
    private String cookieName;

    @Value("${fintracker.security.refresh-token.cookie-secure}")
    private boolean cookieSecure;

    @Value("${fintracker.security.refresh-token.ttl}")
    private Duration refreshTokenTtl;

    public AuthController(
            RegistrationService registrationService,
            LoginService loginService,
            RefreshSessionService refreshSessionService,
            OAuthLoginExchangeService oauthLoginExchangeService) {

        this.registrationService = registrationService;
        this.loginService = loginService;
        this.refreshSessionService = refreshSessionService;
        this.oauthLoginExchangeService = oauthLoginExchangeService;
    }

    /**
     * Registers a new FinTracker user.
     */
    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(
            @Valid @RequestBody RegisterRequest request) {

        RegisterResponse response =
            registrationService.register(request);

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(response);
    }

    /**
     * Authenticates a user and creates a persistent refresh session.
     *
     * The access token is returned in JSON.
     * The refresh token is returned only in an HttpOnly cookie.
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request) {

        LoginResult result = loginService.login(
            request.email(),
            request.password()
        );

        ResponseCookie refreshCookie =
            createRefreshCookie(result.refreshToken());

        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
            .body(result.response());
    }

    /**
     * Exchanges the current refresh token for a new token pair.
     *
     * The old refresh token is invalidated by RefreshSessionService.
     * The replacement refresh token is sent only through an HttpOnly cookie.
     */
    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(
            @CookieValue(
                name = "${fintracker.security.refresh-token.cookie-name}",
                required = false
            ) String rawRefreshToken) {

        try {
            LoginResult result =
                refreshSessionService.refresh(rawRefreshToken);

            ResponseCookie replacementCookie =
                createRefreshCookie(result.refreshToken());

            return ResponseEntity.ok()
                .header(
                    HttpHeaders.SET_COOKIE,
                    replacementCookie.toString()
                )
                .body(result.response());

        } catch (
            InvalidRefreshTokenException |
            RefreshTokenReuseException exception
        ) {
            // Remove the stale cookie from the browser.
            ResponseCookie clearedCookie = clearRefreshCookie();

            // Do not reveal whether the token was expired, invalid,
            // or detected as reused.
            return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .header(
                    HttpHeaders.SET_COOKIE,
                    clearedCookie.toString()
                )
                .build();
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(
                name = "${fintracker.security.refresh-token.cookie-name}",
                required = false
            ) String rawRefreshToken) {

        refreshSessionService.logout(rawRefreshToken);

        return ResponseEntity.noContent()
            .header(HttpHeaders.SET_COOKIE, clearRefreshCookie().toString())
            .build();
    }

    /**
     * Builds the HttpOnly refresh-token cookie.
     */
    private ResponseCookie createRefreshCookie(String refreshToken) {

        return ResponseCookie
            .from(cookieName, refreshToken)
            .httpOnly(true)
            .secure(cookieSecure)
            .sameSite("Lax")
            .path("/api/v1/auth")
            .maxAge(refreshTokenTtl)
            .build();
    }

    /**
     * Expires the refresh-token cookie in the browser.
     */
    private ResponseCookie clearRefreshCookie() {

        return ResponseCookie
            .from(cookieName, "")
            .httpOnly(true)
            .secure(cookieSecure)
            .sameSite("Lax")
            .path("/api/v1/auth")
            .maxAge(Duration.ZERO)
            .build();
    }

    /**
     * Exchanges a one-time Google OAuth handoff code for an access token.
     *
     * The matching refresh token must be present in the HttpOnly cookie.
     */
    @PostMapping("/oauth/exchange")
    public ResponseEntity<LoginResponse> exchangeOAuthHandoff(
            @Valid @RequestBody OAuthExchangeRequest request,
            @CookieValue(name = "${fintracker.security.refresh-token.cookie-name}",
                        required = false) String rawRefreshToken) {

        LoginResponse response = oauthLoginExchangeService.exchange(
                request.code(),
                rawRefreshToken);

        return ResponseEntity.ok(response);
    }

}
