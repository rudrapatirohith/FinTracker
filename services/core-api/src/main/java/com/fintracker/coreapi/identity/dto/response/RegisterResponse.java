package com.fintracker.coreapi.identity.dto.response;

import com.fintracker.coreapi.identity.entity.UserStatus;
import java.util.UUID;

/**
 * Safe response returned after successful registration.
 *
 * Never expose password hashes or authentication secrets.
 */
public record RegisterResponse(UUID id, String email, String fullName, UserStatus status) {}
