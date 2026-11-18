
package com.fintracker.coreapi.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Defines which browser origins are allowed to communicate
 * with the FinTracker backend.
 *
 * CORS is a browser security mechanism. It does not replace
 * authentication, authorization, or CSRF protection.
 */
@Configuration
public class CorsConfig {

    /**
     * Creates the CORS policy used by Spring Security.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${fintracker.security.cors.allowed-origin}")
            String frontendOrigin) {

        CorsConfiguration configuration = new CorsConfiguration();

        /*
         * Allow only our configured frontend.
         *
         * Do not use "*" when allowCredentials is true.
         */
        configuration.setAllowedOrigins(List.of(frontendOrigin));

        /*
         * HTTP methods our frontend may use.
         * OPTIONS is needed for browser preflight requests.
         */
        configuration.setAllowedMethods(List.of(
                "GET",
                "POST",
                "PUT",
                "PATCH",
                "DELETE",
                "OPTIONS"
        ));

        /*
         * Headers the frontend may send.
         *
         * Authorization: carries our access JWT.
         * Content-Type: identifies JSON request bodies.
         * X-XSRF-TOKEN: carries the CSRF token.
         */
        configuration.setAllowedHeaders(List.of(
                "Authorization",
                "Content-Type",
                "X-XSRF-TOKEN"
        ));

        /*
         * Essential for our authentication design.
         *
         * This allows the browser to include cookies, including
         * our HttpOnly refresh-token cookie, in cross-origin fetches.
         */
        configuration.setAllowCredentials(true);

        // Cache successful preflight responses for one hour.
        configuration.setMaxAge(3600L);

        /*
         * Apply this policy to all API endpoints.
         * The OAuth flow itself primarily uses browser redirects.
         */
        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/api/**", configuration);

        return source;
    }
}
