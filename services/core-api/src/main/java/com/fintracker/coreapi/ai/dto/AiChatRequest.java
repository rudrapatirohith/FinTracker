package com.fintracker.coreapi.ai.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

/** A bounded chat turn and a short conversation window; no financial data is accepted from the browser. */
public record AiChatRequest(
        @NotBlank @Size(max = 2000) String message,
        @Size(max = 10) List<@Valid ChatMessage> history) {

    public record ChatMessage(
            @NotBlank @Pattern(regexp = "user|assistant") String role,
            @NotBlank @Size(max = 4000) String content) {}
}
