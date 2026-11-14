package com.fintracker.coreapi.identity.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;

/**
 * Responsible for creating short-lived access tokens.
 *
 * This service does not authenticate users.
 * The login service will authenticate the user first,
 * then call this service to issue an access token.
 */
@Service
public class JwtTokenService {

    private final JwtEncoder jwtEncoder;
    private final Clock clock;
    private final String issuer;
    private final Duration accessTokenTtl;

    public JwtTokenService(
            JwtEncoder jwtEncoder,
            @Value("${fintracker.security.jwt.issuer}") String issuer,
            @Value("${fintracker.security.jwt.access-token-ttl}") Duration accessTokenTtl) {

        this.jwtEncoder = jwtEncoder;
        this.issuer = issuer;
        this.accessTokenTtl = accessTokenTtl;
        this.clock = Clock.systemUTC();
    }

    /**
     * Creates a signed JWT for the supplied authenticated user.
     *
     * The subject (sub) identifies the user by application UUID.
     */
    public String createAccessToken(UUID userId) {

        if (userId == null) {
            throw new IllegalArgumentException("User ID is required.");
        }

        Instant issuedAt = clock.instant();
        Instant expiresAt = issuedAt.plus(accessTokenTtl);

        JwtClaimsSet claims = JwtClaimsSet.builder()
            .issuer(issuer)   // fintracker-core-api
            .subject(userId.toString())  // User UUID
            .issuedAt(issuedAt)
            .expiresAt(expiresAt) // 15 min TTL
            .id(UUID.randomUUID().toString())
            .claim("token_use", "access")
            .build();

        JwsHeader header = JwsHeader
            .with(MacAlgorithm.HS256)
            .build();

        return jwtEncoder.encode(
            JwtEncoderParameters.from(header, claims)
        ).getTokenValue();
    }
}