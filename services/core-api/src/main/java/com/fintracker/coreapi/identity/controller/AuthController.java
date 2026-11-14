package com.fintracker.coreapi.identity.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fintracker.coreapi.identity.dto.request.RegisterRequest;
import com.fintracker.coreapi.identity.dto.response.RegisterResponse;
import com.fintracker.coreapi.identity.service.RegistrationService;
import com.fintracker.coreapi.identity.dto.request.LoginRequest;
import com.fintracker.coreapi.identity.dto.response.LoginResponse;
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
            @Valid @RequestBody LoginRequest request) {

        return ResponseEntity.ok(loginService.login(request));
    }
}