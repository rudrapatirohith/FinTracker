package com.fintracker.coreapi.identity.controller;

import com.fintracker.coreapi.identity.service.AuthenticatedUserProvider;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class CurrentUserController {

    private final AuthenticatedUserProvider authenticatedUserProvider;

    /**
     * Receives the application service/provider used by this controller; constructor injection keeps the dependency explicit and immutable.
     */
    public CurrentUserController(AuthenticatedUserProvider authenticatedUserProvider) {
        this.authenticatedUserProvider = authenticatedUserProvider;
    }

    @GetMapping("/me")
    public CurrentUserResponse getCurrentUser() {
        /**
         * Performs the current user response step for this component.
         */
        return new CurrentUserResponse(authenticatedUserProvider.getRequiredUserId());
    }

    /**
     * Performs the current user response step for this component.
     */
    public record CurrentUserResponse(UUID userId) {}
}
