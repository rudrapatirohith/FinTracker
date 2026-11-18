package com.fintracker.coreapi.identity.dto.response;

/**
 * Temporary result passed from the OAuth completion service
 * to the HTTP success handler.
 *
 * The refresh token must only be sent as an HttpOnly cookie.
 */
public record OAuthLoginCompletion(
        String handoffCode,
        String refreshToken
) {
}