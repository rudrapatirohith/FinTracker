package com.fintracker.coreapi.identity.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;

import com.fintracker.coreapi.identity.dto.request.RegisterRequest;
import com.fintracker.coreapi.identity.dto.response.RegisterResponse;
import com.fintracker.coreapi.identity.service.RegistrationService;
import com.fintracker.coreapi.identity.dto.request.LoginRequest;
import com.fintracker.coreapi.identity.dto.response.LoginResponse;
import com.fintracker.coreapi.identity.service.LoginResult;
import com.fintracker.coreapi.identity.service.LoginService;

import jakarta.validation.Valid;

/**
 * Authentication-related REST endpoints.
 */ 
@RestController // Combination of @Contoller and @Requestbody
@RequestMapping("/api/v1/auth")
@Validated
public class AuthController {

    private final RegistrationService registrationService;
    private final LoginService loginService;
    @Value("${fintracker.security.refresh-token.cookie-name}")
    private String cookieName;

    @Value("${fintracker.security.refresh-token.cookie-secure}")
    private boolean cookieSecure;

    @Value("${fintracker.security.refresh-token.ttl}")
    private Duration refreshTokenTtl;

    public AuthController(
            RegistrationService registrationService,
            LoginService loginService) {
        this.registrationService = registrationService;
        this.loginService = loginService;
    }

    /**
     * Registers a new FinTracker user.
     */
    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(
            @Valid @RequestBody RegisterRequest request) {

        RegisterResponse response = registrationService.register(request);

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(response);
    }

    /* Login FinTracker user */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
        @Valid @RequestBody LoginRequest request
    ) {
        LoginResult result = loginService.login(
            request.email(),
            request.password()
        );

        ResponseCookie refreshCookie = ResponseCookie
            .from(cookieName, result.refreshToken())
            .httpOnly(true) // HttpOnly prevents frontend JavaScript from reading the refresh token
            .secure(cookieSecure)
            .sameSite("Lax") // local starting point, not a complete CSRF strategy.
            .path("/api/v1/auth")
            .maxAge(refreshTokenTtl)
            .build();

        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
            .body(result.response());
    }
}