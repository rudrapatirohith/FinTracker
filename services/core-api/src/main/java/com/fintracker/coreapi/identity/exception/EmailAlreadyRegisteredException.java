package com.fintracker.coreapi.identity.exception;

/**
 * Raised when a user attempts to register with an existing email.
 */
public class EmailAlreadyRegisteredException extends RuntimeException {

    public EmailAlreadyRegisteredException() {
        super("An account with this email address already exists.");
    }
}