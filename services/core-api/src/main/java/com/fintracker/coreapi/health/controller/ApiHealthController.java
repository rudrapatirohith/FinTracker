package com.fintracker.coreapi.health.controller;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes basic health information about the Core API.
 */
@RestController
@RequestMapping("/api/v1/health")
public class ApiHealthController {

    @GetMapping
    public Map<String, String> getHealth() {
        return Map.of(
            "status", "UP",
            "service", "fintracker-core-api"
        );
    }
}