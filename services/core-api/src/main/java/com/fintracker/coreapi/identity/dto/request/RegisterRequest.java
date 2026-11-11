package com.fintracker.coreapi.identity.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for creating a new FinTracker account.
 */
public record RegisterRequest(

    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email address")
    @Size(max = 320, message = "Email cannot exceed 320 characters")
    String email,

    @NotBlank(message = "Password is required")
    @Size(
        min = 12,
        max = 128,
        message = "Password must contain between 12 and 128 characters"
    )
    String password,

    @NotBlank(message = "Full name is required")
    @Size(max = 200, message = "Full name cannot exceed 200 characters")
    String fullName

) {
}