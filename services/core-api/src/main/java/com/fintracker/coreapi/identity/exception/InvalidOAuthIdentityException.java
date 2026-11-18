package com.fintracker.coreapi.identity.exception;

public class InvalidOAuthIdentityException extends RuntimeException {

    public InvalidOAuthIdentityException() {
        super("Google authentication could not be completed.");
    }
}