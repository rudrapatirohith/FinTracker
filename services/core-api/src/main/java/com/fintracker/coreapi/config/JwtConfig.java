package com.fintracker.coreapi.config;

import java.util.Base64;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

@Configuration
public class JwtConfig {

    @Bean
    public SecretKey jwtSecretKey(
            @Value("${fintracker.security.jwt.secret}") String encodedSecret) {

        final byte[] secretBytes;

        try {
            secretBytes = Base64.getDecoder().decode(encodedSecret);
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException(
                "JWT_SECRET must be a valid Base64-encoded value.",
                exception
            );
        }

        // HS256 requires a key of at least 256 bits.
        if (secretBytes.length < 32) {
            throw new IllegalStateException(
                "JWT_SECRET must decode to at least 32 bytes."
            );
        }

        return new SecretKeySpec(secretBytes, "HmacSHA256");
    }

    @Bean
    public JwtEncoder jwtEncoder(SecretKey jwtSecretKey) {
        return new NimbusJwtEncoder(
            new ImmutableSecret<>(jwtSecretKey)
        );
    }

    @Bean
    public JwtDecoder jwtDecoder(
            SecretKey jwtSecretKey,
            @Value("${fintracker.security.jwt.issuer}") String issuer) {

        NimbusJwtDecoder decoder = NimbusJwtDecoder
            .withSecretKey(jwtSecretKey)
            .macAlgorithm(MacAlgorithm.HS256)
            .build();

        decoder.setJwtValidator(
            org.springframework.security.oauth2.jwt.JwtValidators
                .createDefaultWithIssuer(issuer)
        );

        return decoder;
    }
}