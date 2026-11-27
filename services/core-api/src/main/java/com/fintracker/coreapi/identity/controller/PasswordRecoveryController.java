package com.fintracker.coreapi.identity.controller;

import com.fintracker.coreapi.identity.dto.request.ForgotPasswordRequest;
import com.fintracker.coreapi.identity.dto.request.ResetPasswordRequest;
import com.fintracker.coreapi.identity.service.PasswordRecoveryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Public password-recovery endpoints; token possession proves access to the recovery mailbox. */
@RestController
@RequestMapping("/api/v1/auth/password")
public class PasswordRecoveryController {
    private final PasswordRecoveryService service;

    public PasswordRecoveryController(PasswordRecoveryService service) {
        this.service = service;
    }

    @PostMapping("/forgot")
    public ResponseEntity<Void> requestReset(@Valid @RequestBody ForgotPasswordRequest request) {
        service.requestReset(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    @PostMapping("/reset")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        service.resetPassword(request);
        return ResponseEntity.noContent().build();
    }
}
