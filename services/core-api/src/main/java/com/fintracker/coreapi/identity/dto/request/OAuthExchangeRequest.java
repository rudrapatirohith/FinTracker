package com.fintracker.coreapi.identity.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record OAuthExchangeRequest(
        @NotBlank(message = "OAuth handoff code is required.") @Size(max = 128, message = "Invalid OAuth handoff code.")
        String code) {}
