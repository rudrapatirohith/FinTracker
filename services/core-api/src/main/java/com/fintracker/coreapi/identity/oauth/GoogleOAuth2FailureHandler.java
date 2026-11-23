package com.fintracker.coreapi.identity.oauth;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Handles failed Google OAuth2 / OpenID Connect authentication.
 *
 * Spring Security calls this handler when the OAuth login process
 * fails before our GoogleOAuth2SuccessHandler can execute.
 *
 * Examples:
 * - User cancels Google login.
 * - Authorization code is invalid or expired.
 * - Google authentication fails.
 * - OIDC identity validation fails.
 */
@Component
public class GoogleOAuth2FailureHandler implements AuthenticationFailureHandler {

    // Fixed frontend callback URL.
    // Keeping this backend-controlled prevents arbitrary redirects.
    private final String frontendCallbackUrl;

    public GoogleOAuth2FailureHandler(
            @Value("${fintracker.security.oauth.frontend-callback-url}") String frontendCallbackUrl) {

        this.frontendCallbackUrl = frontendCallbackUrl;
    }

    /**
     * Called automatically by Spring Security when OAuth authentication
     * fails.
     *
     * The exception contains internal authentication details.
     * We intentionally do not expose those details to the frontend.
     */
    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request, HttpServletResponse response, AuthenticationException exception)
            throws IOException, ServletException {

        /*
         * The OAuth authorization request may have been stored in
         * a temporary HTTP session.
         *
         * Remove that temporary session because this authentication
         * attempt has failed.
         */
        invalidateTemporaryOAuthSession(request);

        /*
         * Redirect with a generic error code.
         *
         * We do not put exception messages, stack traces, tokens,
         * or provider details into the redirect URL.
         */
        String redirectUrl = UriComponentsBuilder.fromUriString(frontendCallbackUrl)
                .queryParam("error", "google_authentication_failed")
                .build()
                .encode()
                .toUriString();

        response.sendRedirect(redirectUrl);
    }

    /**
     * Removes the temporary session used for the OAuth handshake.
     *
     * This is separate from FinTracker's persistent refresh-token
     * sessions stored in PostgreSQL.
     */
    private void invalidateTemporaryOAuthSession(HttpServletRequest request) {

        HttpSession session = request.getSession(false);

        if (session != null) {
            session.invalidate();
        }
    }
}
