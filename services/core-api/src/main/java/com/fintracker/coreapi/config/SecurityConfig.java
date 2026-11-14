package com.fintracker.coreapi.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import com.nimbusds.jwt.JWT;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {

        http
            // Registration & Login is public and does not use cookie authentication.
            .csrf(csrf -> csrf.ignoringRequestMatchers(
                new AntPathRequestMatcher("/api/v1/auth/register", "POST"),
                new AntPathRequestMatcher("/api/v1/auth/login", "POST")
            ))

            // This API authenticates requests using JWTs, not HTTP sessions.
            // STATELESS: Spring Security won't create or maintain an HTTP session for API authentication.
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            // Define which endpoints are public and which require authentication.
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    HttpMethod.GET,
                    "/api/v1/health",
                    "/api/v1/health/ready"
                ).permitAll()

                .requestMatchers(
                    HttpMethod.POST,
                    "/api/v1/auth/register",
                    "/api/v1/auth/login"
                ).permitAll()

                .anyRequest().authenticated()
            )

            // Activate Spring Security's JWT bearer-token authentication.
            // oauth2ResourceServer().jwt(): activates the JWT authentication filter.
            // JwtDecoder: Spring automatically uses the decoder we configured earlier.
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(Customizer.withDefaults())
            );

        return http.build();
    }
}