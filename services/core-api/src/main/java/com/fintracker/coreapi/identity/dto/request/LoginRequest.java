package com.fintracker.coreapi.identity.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Credentials submitted when a user logs in.
 */
public record LoginRequest(

    @NotBlank(message = "Email is required.")
    @Email(message = "Enter a valid email address.")
    @Size(max = 320, message = "Email is too long.")
    String email,

    @NotBlank(message = "Password is required.")
    @Size(max = 128, message = "Password is too long.")
    String password

) {
}