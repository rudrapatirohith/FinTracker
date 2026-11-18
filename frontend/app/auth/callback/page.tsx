
"use client";

/**
 * Google OAuth Callback Page
 *
 * Google redirects the browser to this page after the user approves
 * the sign-in request.
 *
 * The callback URL contains a temporary authorization code.
 *
 * Our responsibility is to:
 * 1. Read the temporary authorization code.
 * 2. Send it to Spring Boot for verification and exchange.
 * 3. Store the returned application session through AuthProvider.
 * 4. Redirect the authenticated user to the dashboard.
 *
 * Important:
 * The authorization code is temporary and can only be exchanged once.
 */

import { useEffect, useRef, useState } from "react";
import { useRouter } from "next/navigation";

import { useAuth } from "@/components/auth/auth-provider";
import { AUTH_API, fetchWithCsrf, type LoginResponse } from "@/lib/api";

import { Loader2 } from "lucide-react";

export default function OAuthCallbackPage() {
  const router = useRouter();

  // completeLogin updates the global authentication state.
  const { completeLogin } = useAuth();

  // Prevent the OAuth code from being exchanged more than once.
  // This is important because React Strict Mode can run effects twice
  // during development.
  const exchangeStartedRef = useRef(false);

  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    // Do not start another exchange if one is already in progress.
    if (exchangeStartedRef.current) {
      return;
    }

    // Read the temporary authorization code from the callback URL.
    const callbackUrl = new URL(window.location.href);
    const authorizationCode = callbackUrl.searchParams.get("code");

    // Remove the temporary code from the visible browser URL.
    // We captured it above, so we can safely remove it now.
    window.history.replaceState({}, document.title, "/auth/callback");

    // Google should have supplied an authorization code.
    if (!authorizationCode) {
      setError(
        "Google sign-in did not return an authorization code. Please try again."
      );
      return;
    }

    // Mark the exchange as started before making the network request.
    exchangeStartedRef.current = true;

    const exchangeAuthorizationCode = async () => {
      try {
        /**
         * Send the temporary Google authorization code to Spring Boot.
         *
         * fetchWithCsrf:
         * - Retrieves the CSRF token.
         * - Adds the required CSRF header.
         * - Includes cookies in the request.
         */
        const response = await fetchWithCsrf(
          AUTH_API.oauthExchange,
          {
            method: "POST",
            headers: {
              "Content-Type": "application/json",
            },
            body: JSON.stringify({
              code: authorizationCode,
            }),
          }
        );

        // Read the backend response.
        const responseBody = await response.json();

        // Stop if Spring Boot rejected the authorization-code exchange.
        if (!response.ok) {
          throw new Error(
            responseBody.message ||
              responseBody.error ||
              `Google authentication failed. HTTP status: ${response.status}`
          );
        }

        // The backend should return our application's login response.
        const loginResponse = responseBody as LoginResponse;

        // Validate the important fields before establishing the session.
        if (
          !loginResponse.accessToken ||
          !loginResponse.userId ||
          !loginResponse.email
        ) {
          throw new Error(
            "The backend returned an incomplete authentication response."
          );
        }

        /**
         * Establish the authenticated session in our global AuthProvider.
         *
         * This updates the user and access token so the dashboard
         * recognizes the user as authenticated.
         */
        completeLogin(loginResponse);

        // Authentication succeeded. Navigate to the dashboard.
        router.replace("/dashboard");
      } catch (err: unknown) {
        console.error("Google OAuth callback error:", err);

        setError(
          err instanceof Error
            ? err.message
            : "An unexpected error occurred during Google sign-in."
        );
      }
    };

    exchangeAuthorizationCode();
  }, [completeLogin, router]);

  // Display an error if the authorization-code exchange fails.
  if (error) {
    return (
      <div className="flex min-h-screen flex-col items-center justify-center gap-4 px-6 text-center">
        <h1 className="text-xl font-semibold">
          Google Sign-In Failed
        </h1>

        <p className="max-w-md text-sm text-muted-foreground">
          {error}
        </p>

        <button
          onClick={() => router.replace("/")}
          className="rounded-md bg-primary px-5 py-2 text-sm font-medium text-primary-foreground"
        >
          Return to Sign In
        </button>
      </div>
    );
  }

  // Show a loading screen while Spring Boot processes the code.
  return (
    <div className="flex min-h-screen flex-col items-center justify-center gap-4">
      <Loader2 className="h-8 w-8 animate-spin text-primary" />

      <p className="text-sm text-muted-foreground">
        Completing Google sign-in...
      </p>
    </div>
  );
}
