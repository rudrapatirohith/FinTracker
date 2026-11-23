package com.fintracker.coreapi.identity.service;

import com.fintracker.coreapi.identity.dto.response.LoginResponse;

/**
 * Internal result returned by the login service.
 *
 * The refresh token is used only to create the HttpOnly cookie.
 * It must never be serialized into the API response body.
 */
public record LoginResult(LoginResponse response, String refreshToken) {}
