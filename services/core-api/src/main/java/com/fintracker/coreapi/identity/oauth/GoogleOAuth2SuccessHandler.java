
package com.fintracker.coreapi.identity.oauth;

import java.io.IOException;
import java.time.Duration;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import com.fintracker.coreapi.identity.dto.response.OAuthLoginCompletion;
import com.fintracker.coreapi.identity.exception.OAuthAccountLinkRequiredException;
import com.fintracker.coreapi.identity.service.OAuthLoginCompletionService;

/**
 * Handles a successful Google OAuth2 / OpenID Connect login.
 *
 * IMPORTANT:
 * Google authenticates the user, but FinTracker must establish
 * its own application session.
 *
 * This handler connects Google's authenticated identity to
 * FinTracker's user, refresh-token, and handoff systems.
 */
@Component
public class GoogleOAuth2SuccessHandler
        implements AuthenticationSuccessHandler {

    private final OAuthLoginCompletionService completionService;

    // Fixed frontend callback URL configured by the backend.
    // We never accept an arbitrary redirect URL from the browser.
    private final String frontendCallbackUrl;

    // Refresh-token cookie configuration.
    private final String refreshCookieName;
    private final boolean refreshCookieSecure;
    private final Duration refreshTokenTtl;

    public GoogleOAuth2SuccessHandler(
            OAuthLoginCompletionService completionService,

            @Value("${fintracker.security.oauth.frontend-callback-url}")
            String frontendCallbackUrl,

            @Value("${fintracker.security.refresh-token.cookie-name}")
            String refreshCookieName,

            @Value("${fintracker.security.refresh-token.cookie-secure}")
            boolean refreshCookieSecure,

            @Value("${fintracker.security.refresh-token.ttl}")
            Duration refreshTokenTtl) {

        this.completionService = completionService;
        this.frontendCallbackUrl = frontendCallbackUrl;
        this.refreshCookieName = refreshCookieName;
        this.refreshCookieSecure = refreshCookieSecure;
        this.refreshTokenTtl = refreshTokenTtl;
    }

    /**
     * Spring Security calls this method automatically after
     * successful OAuth authentication.
     *
     * The Authentication object contains the authenticated
     * Google identity.
     */
    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication)
            throws IOException, ServletException {

        try {

            /*
             * STEP 1: Verify that Spring authenticated an OIDC user.
             *
             * OidcUser represents the identity returned through
             * OpenID Connect. We need this identity to resolve
             * the corresponding FinTracker account.
             */
            if (!(authentication.getPrincipal() instanceof OidcUser oidcUser)) {

                redirectWithError(response, "oauth_login_failed");
                return;
            }

            /*
             * STEP 2: Complete FinTracker authentication.
             *
             * This service:
             *   1. Resolves or creates the FinTracker user.
             *   2. Creates a persistent refresh-token session.
             *   3. Creates a one-time handoff code.
             *
             * Database operations are coordinated by the
             * OAuthLoginCompletionService transaction.
             */
            OAuthLoginCompletion completion =
                    completionService.completeLogin(oidcUser);

            /*
             * STEP 3: Store the refresh token in an HttpOnly cookie.
             *
             * HttpOnly prevents JavaScript from reading this cookie.
             * Secure controls whether the browser sends it only over HTTPS.
             * SameSite=Lax provides additional cross-site request protection.
             *
             * The refresh token is NOT placed in the redirect URL.
             */
            addRefreshCookie(response, completion.refreshToken());

            /*
             * STEP 4: Redirect the browser to the frontend.
             *
             * Only the short-lived, single-use handoff code is included.
             * The frontend will exchange it for an access JWT through
             * our protected-by-CSRF API flow.
             */
            String redirectUrl = UriComponentsBuilder
                    .fromUriString(frontendCallbackUrl)
                    .queryParam("code", completion.handoffCode())
                    .build()
                    .encode()
                    .toUriString();

            /*
             * STEP 5: Remove the temporary OAuth HTTP session.
             *
             * Spring Security uses a temporary session during
             * the authorization handshake to retain and validate
             * the OAuth authorization request and state.
             *
             * It is not our permanent FinTracker login session.
             */
            invalidateTemporaryOAuthSession(request);

            response.sendRedirect(redirectUrl);

        } catch (OAuthAccountLinkRequiredException exception) {

            /*
             * The email already belongs to an existing FinTracker
             * account. We do not automatically link Google to it.
             *
             * The user must first authenticate using their existing
             * account and then explicitly link Google.
             */
            invalidateTemporaryOAuthSession(request);

            redirectWithError(response, "account_link_required");

        } catch (RuntimeException exception) {

            /*
             * Do not expose internal exception details, database
             * information, or authentication implementation details
             * to the frontend.
             *
             * Detailed logging and monitoring will be added separately.
             */
            invalidateTemporaryOAuthSession(request);

            redirectWithError(response, "oauth_login_failed");
        }
    }

    /**
     * Creates the browser's refresh-token cookie.
     *
     * The cookie is restricted to the authentication API path,
     * so the browser sends it to our auth endpoints rather than
     * exposing it to frontend JavaScript.
     */
    private void addRefreshCookie(
            HttpServletResponse response,
            String refreshToken) {

        ResponseCookie cookie = ResponseCookie
                .from(refreshCookieName, refreshToken)
                .httpOnly(true)
                .secure(refreshCookieSecure)
                .path("/api/v1/auth")
                .sameSite("Lax")
                .maxAge(refreshTokenTtl)
                .build();

        response.addHeader(
                HttpHeaders.SET_COOKIE,
                cookie.toString());
    }

    /**
     * Redirects to the fixed frontend callback with a controlled
     * error code instead of exposing internal exception messages.
     */
    private void redirectWithError(
            HttpServletResponse response,
            String errorCode) throws IOException {

        String redirectUrl = UriComponentsBuilder
                .fromUriString(frontendCallbackUrl)
                .queryParam("error", errorCode)
                .build()
                .encode()
                .toUriString();

        response.sendRedirect(redirectUrl);
    }

    /**
     * Invalidates the temporary HTTP session used during OAuth.
     *
     * This does not revoke the persistent FinTracker refresh-token
     * session stored in PostgreSQL.
     */
    private void invalidateTemporaryOAuthSession(
            HttpServletRequest request) {

        HttpSession session = request.getSession(false);

        if (session != null) {
            session.invalidate();
        }
    }
}
