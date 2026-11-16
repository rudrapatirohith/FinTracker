package com.fintracker.coreapi.identity.exception;

public class RefreshTokenReuseException extends RuntimeException {

    public RefreshTokenReuseException() {
        super("Refresh token reuse detected. The session family has been revoked.");
    }
}