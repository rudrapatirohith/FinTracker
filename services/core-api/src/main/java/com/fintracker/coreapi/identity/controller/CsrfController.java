package com.fintracker.coreapi.identity.controller;

import java.util.Map;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Provides the CSRF token required by browser clients
 * for cookie-authenticated requests.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class CsrfController {

    @GetMapping("/csrf")
    public Map<String, String> getCsrfToken(CsrfToken csrfToken) {

        return Map.of(
                "token", csrfToken.getToken(),
                "headerName", csrfToken.getHeaderName());
    }
}
