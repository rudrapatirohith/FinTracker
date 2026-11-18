package com.fintracker.coreapi.identity.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Returns information about the currently authenticated user.
 *
 * The user identity comes from the verified JWT.
 * We never accept a user ID from the request to determine identity.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class CurrentUserController {

    @GetMapping("/me")
    public CurrentUserResponse getCurrentUser(
            @AuthenticationPrincipal Jwt jwt) { // Instructs Spring Security to inject the decoded, cryptographically verified Jwt object from the current SecurityContext directly into the method parameter.

        try {
            UUID userId = UUID.fromString(jwt.getSubject()); // Reads the sub claim from the token payload, which your JwtTokenService populated with the user's immutable database UUID.

            return new CurrentUserResponse(userId);

        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Invalid authenticated user identity."
            );
        }
    }

    public record CurrentUserResponse(UUID userId) {
    }
}