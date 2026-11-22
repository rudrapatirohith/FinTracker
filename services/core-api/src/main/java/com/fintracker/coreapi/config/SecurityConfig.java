
package com.fintracker.coreapi.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

import com.fintracker.coreapi.identity.oauth.GoogleOAuth2FailureHandler;
import com.fintracker.coreapi.identity.oauth.GoogleOAuth2SuccessHandler;

/**
 * Defines the security rules for FinTracker.
 *
 * We use separate filter chains because OAuth login temporarily
 * requires an HTTP session, while our REST API uses stateless JWT
 * authentication.
 */
@Configuration
public class SecurityConfig {

    /**
     * Creates the CSRF token repository used by the REST API.
     *
     * The repository stores the CSRF token in a browser cookie.
     * JavaScript can read the CSRF cookie and send its value in
     * the X-XSRF-TOKEN request header.
     *
     * This cookie is intentionally not HttpOnly because the frontend
     * needs to read the CSRF token. This is NOT our refresh-token cookie.
     */
    @Bean
    public CookieCsrfTokenRepository csrfTokenRepository(
            @Value("${fintracker.security.refresh-token.cookie-secure}") boolean cookieSecure,
            @Value("${fintracker.security.refresh-token.cookie-same-site}") String cookieSameSite) {

        CookieCsrfTokenRepository repository =
                CookieCsrfTokenRepository.withHttpOnlyFalse();

        // Allow the frontend to use the CSRF cookie across API paths.
        repository.setCookiePath("/");
        repository.setCookieCustomizer(cookie -> cookie.secure(cookieSecure).sameSite(cookieSameSite));

        return repository;
    }

    /**
     * FILTER CHAIN 1: Google OAuth authentication.
     *
     * Handles:
     *   /oauth2/authorization/google
     *   /login/oauth2/code/google
     *
     * These endpoints are handled by Spring Security's OAuth2 client.
     */
    @Bean
    @Order(1)
    public SecurityFilterChain oauthSecurityFilterChain(
            HttpSecurity http,
            OAuth2AuthorizationRequestResolver authorizationRequestResolver,
            GoogleOAuth2SuccessHandler successHandler,
            GoogleOAuth2FailureHandler failureHandler)
            throws Exception {

        http
            /*
             * This chain handles only OAuth authorization and callback URLs.
             * Requests to /api/** will not enter this chain.
             */
            .securityMatcher("/oauth2/**", "/login/oauth2/**")

            /*
             * OAuth requires a temporary HTTP session to preserve
             * the authorization request, state, and PKCE verifier.
             *
             * This session is not our permanent application session.
             */
            .sessionManagement(session -> session
                    .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))

            /*
             * Configure Spring Security's OAuth2 / OIDC login process.
             */
            .oauth2Login(oauth2 -> oauth2

                    /*
                     * Connect our PKCE-enabled resolver.
                     * Without this, declaring the resolver bean alone
                     * would not guarantee that OAuth requests use PKCE.
                     */
                    .authorizationEndpoint(authorization -> authorization
                            .authorizationRequestResolver(
                                    authorizationRequestResolver))

                    /*
                     * Run our application-specific logic after Google
                     * authentication succeeds or fails.
                     */
                    .successHandler(successHandler)
                    .failureHandler(failureHandler)
            )

            /*
             * These endpoints are part of the OAuth login handshake.
             * They must be reachable before the user is authenticated.
             */
            .authorizeHttpRequests(auth -> auth
                    .anyRequest().permitAll());

        return http.build();
    }

    /**
     * FILTER CHAIN 2: FinTracker REST API.
     *
     * Every /api/** request enters this chain.
     * Authentication is based on our own JWT access tokens.
     */
    @Bean
    @Order(2)
    public SecurityFilterChain apiSecurityFilterChain(
            HttpSecurity http,
            CookieCsrfTokenRepository csrfTokenRepository)
            throws Exception {

        http

            /*
             * Restrict this chain to our versioned API routes.
             */
            .securityMatcher("/api/**")

            // Apply the CORS policy defined in CorsConfig.
            .cors(Customizer.withDefaults())
    
            /*
             * Keep CSRF protection enabled.
             *
             * Registration is exempt because it does not depend on
             * an existing browser authentication cookie.
             *
             * Login, refresh, logout, and OAuth exchange remain protected.
             */
            .csrf(csrf -> csrf
                    .csrfTokenRepository(csrfTokenRepository)
                    .csrfTokenRequestHandler(
                            new CsrfTokenRequestAttributeHandler())
                    .ignoringRequestMatchers("/api/v1/auth/register"))

            /*
             * The API does not use server-side HTTP sessions.
             *
             * Each protected request must carry a valid access JWT.
             */
            .sessionManagement(session -> session
                    .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            /*
             * Define which API endpoints are public and which require
             * an authenticated FinTracker user.
             */
            .authorizeHttpRequests(auth -> auth

                    // Public health and CSRF-token endpoints.
                    .requestMatchers(HttpMethod.GET,
                            "/api/v1/health",
                            "/api/v1/health/ready",
                            "/api/v1/auth/csrf")
                    .permitAll()

                    // Public authentication endpoints.
                    .requestMatchers(HttpMethod.POST,
                            "/api/v1/auth/register",
                            "/api/v1/auth/login",
                            "/api/v1/auth/refresh",
                            "/api/v1/auth/logout",
                            "/api/v1/auth/oauth/exchange")
                    .permitAll()

                    // All other API endpoints require authentication.
                    .anyRequest().authenticated()
            )

            /*
             * Configure JWT validation.
             *
             * Spring Security reads the Authorization: Bearer header,
             * validates the JWT using our JwtDecoder, and creates the
             * authenticated SecurityContext.
             */
            .oauth2ResourceServer(oauth2 -> oauth2
                    .jwt(Customizer.withDefaults()));

        return http.build();
    }

    /**
     * FILTER CHAIN 3: Default deny.
     *
     * If a request does not match either the OAuth or API chain,
     * it reaches this fallback chain.
     *
     * This prevents unexpected routes from accidentally becoming public.
     */
    @Bean
    @Order(3)
    public SecurityFilterChain fallbackSecurityFilterChain(
            HttpSecurity http) throws Exception {

        http
            .authorizeHttpRequests(auth -> auth
                    .anyRequest().denyAll());

        return http.build();
    }
}
