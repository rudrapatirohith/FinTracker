package com.fintracker.coreapi.identity.exception;

public class OAuthAccountLinkRequiredException extends RuntimeException {

    public OAuthAccountLinkRequiredException() {
        super("This email already has an account. Sign in using your existing method and link Google from account settings.");
    }
}