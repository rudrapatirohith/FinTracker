
package com.fintracker.coreapi.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Defines the initial API security policy.
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {

        http.authorizeHttpRequests(auth -> auth
            .requestMatchers(
                HttpMethod.GET,
                "/api/v1/health",
                "/api/v1/health/ready"
            ).permitAll()
            .anyRequest().denyAll()
        );

        return http.build();
    }
}
