package com.fintracker.coreapi.identity.dto.response;

import java.util.UUID;

import com.fintracker.coreapi.identity.entity.UserStatus;

/**
 * Safe response returned after successful registration.
 *
 * Never expose password hashes or authentication secrets.
 */
public record RegisterResponse(
    UUID id,
    String email,
    String fullName,
    UserStatus status
) {
}