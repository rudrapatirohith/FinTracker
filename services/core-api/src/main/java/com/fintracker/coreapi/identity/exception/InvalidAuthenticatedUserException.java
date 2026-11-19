package com.fintracker.coreapi.identity.exception;

public class InvalidAuthenticatedUserException extends RuntimeException {

    public InvalidAuthenticatedUserException() {
        super("The authenticated user identity is invalid.");
    }
}
