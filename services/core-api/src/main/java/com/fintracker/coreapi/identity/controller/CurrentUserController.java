/**
 * Small identity endpoint that returns the user ID associated with the current authenticated JWT.
 * It delegates token interpretation to AuthenticatedUserProvider instead of reading claims independently, ensuring identity resolution follows the same validation rules used by financial services.
 */
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

    public CurrentUserController(AuthenticatedUserProvider authenticatedUserProvider) {
        this.authenticatedUserProvider = authenticatedUserProvider;
    }

    @GetMapping("/me")
    public CurrentUserResponse getCurrentUser() {
        return new CurrentUserResponse(authenticatedUserProvider.getRequiredUserId());
    }

    public record CurrentUserResponse(UUID userId) {}
}
