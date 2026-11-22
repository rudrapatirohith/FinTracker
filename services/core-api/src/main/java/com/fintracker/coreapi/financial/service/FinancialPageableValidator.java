package com.fintracker.coreapi.financial.service;

import java.util.Set;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Validates client-supplied pagination sort properties against each resource's explicit allowlist. */
public final class FinancialPageableValidator {
    private FinancialPageableValidator() {}

    /** Rejects unknown or internal entity properties before they reach Spring Data query construction. */
    public static void validate(Pageable pageable, Set<String> allowedProperties) {
        pageable.getSort().forEach(order -> {
            if (!allowedProperties.contains(order.getProperty())) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Unsupported sort property: " + order.getProperty());
            }
        });
    }
}
