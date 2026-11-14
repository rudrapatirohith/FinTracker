package com.fintracker.coreapi.identity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.UUID;

import com.fintracker.coreapi.identity.service.JwtTokenService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class JwtTokenServiceTest {

    @Autowired
    private JwtTokenService jwtTokenService;

    @Autowired
    private JwtDecoder jwtDecoder;

    @Test
    void shouldCreateValidAccessToken() {

        UUID userId = UUID.randomUUID();

        String token = jwtTokenService.createAccessToken(userId);

        assertNotNull(token);

        Jwt decodedToken = jwtDecoder.decode(token);

        assertEquals(userId.toString(), decodedToken.getSubject());
        assertEquals(
            "fintracker-core-api-test",
            decodedToken.getClaimAsString("iss")
        );
        assertEquals("access", decodedToken.getClaimAsString("token_use"));

        assertNotNull(decodedToken.getId());
        assertTrue(
            decodedToken.getExpiresAt().isAfter(decodedToken.getIssuedAt())
        );
    }

    @Test
    void accessTokenShouldExpireAfterFifteenMinutes() {

        String token = jwtTokenService.createAccessToken(UUID.randomUUID());

        Jwt decodedToken = jwtDecoder.decode(token);

        Duration tokenLifetime = Duration.between(
            decodedToken.getIssuedAt(),
            decodedToken.getExpiresAt()
        );

        assertEquals(Duration.ofMinutes(15), tokenLifetime);
    }

    @Test
    void shouldRejectTokenWithModifiedSignature() {

        String token = jwtTokenService.createAccessToken(UUID.randomUUID());

        String[] parts = token.split("\\.");

        String incorrectSignature = Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(
                "incorrect-signature".getBytes(StandardCharsets.UTF_8)
            );

        String tamperedToken =
            parts[0] + "." + parts[1] + "." + incorrectSignature;

        assertThrows(
            JwtException.class,
            () -> jwtDecoder.decode(tamperedToken)
        );
    }

    @Test
    void shouldRejectMalformedToken() {

        assertThrows(
            JwtException.class,
            () -> jwtDecoder.decode("this-is-not-a-jwt")
        );
    }

    @Test
    void shouldRejectNullUserId() {

        assertThrows(
            IllegalArgumentException.class,
            () -> jwtTokenService.createAccessToken(null)
        );
    }
}