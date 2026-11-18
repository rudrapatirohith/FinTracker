package com.fintracker.coreapi.identity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fintracker.coreapi.identity.service.RefreshTokenService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RefreshTokenServiceTest {

    private RefreshTokenService refreshTokenService;

    @BeforeEach
    void setUp() {
        refreshTokenService = new RefreshTokenService();
    }

    @Test
    void shouldGenerateSecureRefreshToken() {

        String token = refreshTokenService.generateToken();

        assertTrue(token != null && !token.isBlank());

        // 32 bytes encoded as URL-safe Base64 without padding = 43 chars.
        assertEquals(43, token.length());
    }

    @Test
    void shouldGenerateDifferentTokens() {

        String firstToken = refreshTokenService.generateToken();
        String secondToken = refreshTokenService.generateToken();

        assertNotEquals(firstToken, secondToken);
    }

    @Test
    void shouldGenerateConsistentSha256Hash() {

        String token = refreshTokenService.generateToken();

        String firstHash = refreshTokenService.hashToken(token);
        String secondHash = refreshTokenService.hashToken(token);

        assertEquals(firstHash, secondHash);
        assertEquals(64, firstHash.length());
    }

    @Test
    void differentTokensShouldHaveDifferentHashes() {

        String firstToken = refreshTokenService.generateToken();
        String secondToken = refreshTokenService.generateToken();

        assertNotEquals(
            refreshTokenService.hashToken(firstToken),
            refreshTokenService.hashToken(secondToken)
        );
    }

    @Test
    void shouldRejectNullToken() {

        assertThrows(
            IllegalArgumentException.class,
            () -> refreshTokenService.hashToken(null)
        );
    }

    @Test
    void shouldRejectEmptyToken() {

        assertThrows(
            IllegalArgumentException.class,
            () -> refreshTokenService.hashToken(" ")
        );
    }
}