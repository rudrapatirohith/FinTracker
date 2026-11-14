package com.fintracker.coreapi.identity.dto.response;

import java.util.UUID;

/**
 * Response returned after successful authentication.
 *
 * Never include password hashes or refresh-token hashes here.
 */
public record LoginResponse(
    String accessToken,
    String tokenType,
    long expiresInSeconds,
    UUID userId,
    String email
) {
}