package com.fintracker.coreapi.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestCustomizers;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;

/**
 * Configures additional behavior for OAuth authorization requests.
 *
 * PKCE (Proof Key for Code Exchange) protects the authorization-code flow
 * against interception when the temporary code passes through the browser.
 *
 * How the 5-step PKCE handshake works automatically under the hood:
 *   1. Before redirecting to Google, Spring generates a random secret (`code_verifier`)
 *      and stores it safely in the server's temporary HTTP session.
 *   2. Spring derives a one-way SHA-256 hash (`code_challenge`) from that secret.
 *   3. Google receives `code_challenge` in the redirect URL and remembers it.
 *   4. When the user returns with an authorization code, Spring sends that code plus
 *      the original `code_verifier` directly to Google's token endpoint (server-to-server).
 *   5. Google hashes the `code_verifier` and verifies it matches Step 3's `code_challenge`.
 *      An intercepted authorization code alone cannot be exchanged without the secret verifier.
 */
@Configuration
public class OAuth2ClientConfig {

    /**
     * Creates the component Spring Security uses when constructing
     * an authorization request to Google.
     *
     * Note: Declaring this bean alone is not enough—it must be wired into
     * Filter Chain 1 (`oauth2Login().authorizationEndpoint()`) in SecurityConfig.
     */
    @Bean
    public OAuth2AuthorizationRequestResolver authorizationRequestResolver(
            ClientRegistrationRepository clientRegistrationRepository) {

        /*
         * Spring's default resolver intercepts URLs matching:
         *   /oauth2/authorization/{registrationId} (e.g., /oauth2/authorization/google)
         */
        DefaultOAuth2AuthorizationRequestResolver resolver =
                new DefaultOAuth2AuthorizationRequestResolver(clientRegistrationRepository, "/oauth2/authorization");

        /*
         * Enables PKCE (Steps 1–5 above):
         * - Appends `code_challenge` and `code_challenge_method=S256` to the Google redirect URL.
         * - Saves the matching `code_verifier` in the temporary HTTP session for the token exchange.
         */
        resolver.setAuthorizationRequestCustomizer(OAuth2AuthorizationRequestCustomizers.withPkce());

        return resolver;
    }
}
