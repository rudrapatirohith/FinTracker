package com.fintracker.coreapi.identity.exception;

public class InvalidOAuthHandoffException extends RuntimeException {

    public InvalidOAuthHandoffException() {
        super("OAuth login could not be completed. Please sign in again.");
    }
}