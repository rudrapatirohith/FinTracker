package com.fintracker.coreapi.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

@Configuration
public class SecurityConfig {

    @Bean
    public CookieCsrfTokenRepository csrfTokenRepository() {
        CookieCsrfTokenRepository repository = CookieCsrfTokenRepository.withHttpOnlyFalse();

        // The frontend must be able to read this cookie to obtain
        // the CSRF token and send it in the X-XSRF-TOKEN header.
        repository.setCookiePath("/");

        return repository;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            CookieCsrfTokenRepository csrfTokenRepository)
            throws Exception {

        http
                // Cookie-based refresh authentication requires CSRF protection.
                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfTokenRepository)
                        .csrfTokenRequestHandler(
                                new CsrfTokenRequestAttributeHandler())

                        // Registration does not authenticate the browser or
                        // establish an authenticated cookie session.
                        .ignoringRequestMatchers(
                                "/api/v1/auth/register"))

                // This API authenticates requests using JWTs, not HTTP sessions.
                // STATELESS: Spring Security won't create or maintain an HTTP session for API
                // authentication.
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Define which endpoints are public and which require authentication.
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/health",
                                "/api/v1/health/ready",
                                "/api/v1/auth/csrf")
                        .permitAll()

                        // These endpoints are publicly reachable, but login and
                        // refresh still require a valid CSRF token.
                        .requestMatchers(HttpMethod.POST,
                                "/api/v1/auth/register",
                                "/api/v1/auth/login",
                                "/api/v1/auth/refresh",
                                "/api/v1/auth/logout")
                        .permitAll()

                        .anyRequest().authenticated())

                // Activate Spring Security's JWT bearer-token authentication.
                // oauth2ResourceServer().jwt(): activates the JWT authentication filter.
                // JwtDecoder: Spring automatically uses the decoder we configured earlier.
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(Customizer.withDefaults()));

        return http.build();
    }
}
