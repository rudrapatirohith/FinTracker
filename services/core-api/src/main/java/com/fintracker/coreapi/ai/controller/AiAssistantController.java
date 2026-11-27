package com.fintracker.coreapi.ai.controller;

import com.fintracker.coreapi.ai.dto.AiChatRequest;
import com.fintracker.coreapi.ai.dto.AiChatResponse;
import com.fintracker.coreapi.ai.service.AiAssistantService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Authenticated API boundary for the financial assistant. */
@RestController
@RequestMapping("/api/v1/ai")
public class AiAssistantController {
    private final AiAssistantService service;

    public AiAssistantController(AiAssistantService service) {
        this.service = service;
    }

    @PostMapping("/chat")
    public AiChatResponse chat(@Valid @RequestBody AiChatRequest request) {
        return service.chat(request);
    }
}
