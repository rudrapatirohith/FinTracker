package com.fintracker.coreapi.identity.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Validated profile fields that the signed-in user is allowed to change. */
public record UserProfileUpdateRequest(
        @Size(max = 200) String fullName,
        @NotBlank @Pattern(regexp = "[A-Za-z]{3}") String currencyPreference,
        @NotBlank @Size(max = 64) String timezone) {}
