package com.fintracker.coreapi.identity.service;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Base64;

import org.springframework.stereotype.Service;

/**
 * Generates cryptographically secure refresh tokens
 * and creates SHA-256 hashes for database storage.
 */
@Service
public class RefreshTokenService {

    // Unlike java.util.Random (which is predictable), SecureRandom uses OS-level cryptographic entropy to fill a 32-byte array. Guessing a 256-bit random token by brute force is mathematically infeasible.
    private final SecureRandom secureRandom = new SecureRandom(); 

    /**
     * Generates 32 random bytes (256 bits of entropy).
     *
     * URL-safe Base64 makes the token suitable for an HTTP cookie.
     */
    public String generateToken() {

        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);

        return Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(randomBytes);
    }

    /**
     * Hashes a refresh token using SHA-256.
     * Passwords are human-created and often predictable. They need slow password hashing such as Argon.
       Refresh tokens are machine-generated with high entropy. SHA-256 is appropriate and efficient.
     * We store this hash in PostgreSQL, never the original token.
     */
    public String hashToken(String token) {

        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException(
                "Refresh token cannot be empty."
            );
        }

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            byte[] hashBytes = digest.digest(
                token.getBytes(java.nio.charset.StandardCharsets.UTF_8)
            );

            return HexFormat.of().formatHex(hashBytes);

        } catch (java.security.NoSuchAlgorithmException exception) {
            // SHA-256 is required by the Java platform.
            throw new IllegalStateException(
                "SHA-256 algorithm is unavailable.",
                exception
            );
        }
    }
}