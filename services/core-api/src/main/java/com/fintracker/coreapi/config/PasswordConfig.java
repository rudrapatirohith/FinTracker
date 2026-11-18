package com.fintracker.coreapi.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Central configuration for password hashing.
 *
 * We never store plain-text passwords.
 */
@Configuration
public class PasswordConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        // Uses Argon2id with Spring Security's recommended defaults.
        return Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();
    }
}