package com.fintracker.coreapi.identity.exception;

/**
 * Used when the submitted credentials cannot authenticate a user.
 *
 * The same exception is used for unknown accounts, incorrect passwords,
 * and accounts that are not allowed to log in.
 */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("Invalid email or password.");
    }
}
