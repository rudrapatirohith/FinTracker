package com.fintracker.coreapi.identity.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Email address used to request a one-time password-reset link. */
public record ForgotPasswordRequest(
        @NotBlank @Email @Size(max = 320) String email) {}
