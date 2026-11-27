package com.fintracker.coreapi.identity.controller;

import com.fintracker.coreapi.identity.dto.request.UserProfileUpdateRequest;
import com.fintracker.coreapi.identity.dto.response.UserProfileResponse;
import com.fintracker.coreapi.identity.service.UserProfileService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** HTTP boundary for the current user's profile preferences. */
@RestController
@RequestMapping("/api/v1/profile")
public class UserProfileController {
    private final UserProfileService service;

    public UserProfileController(UserProfileService service) {
        this.service = service;
    }

    @GetMapping
    public UserProfileResponse getCurrentProfile() {
        return service.getCurrentProfile();
    }

    @PutMapping
    public UserProfileResponse updateCurrentProfile(@Valid @RequestBody UserProfileUpdateRequest request) {
        return service.updateCurrentProfile(request);
    }
}
