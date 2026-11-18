/**
 * Shared API utilities for communicating with the Spring Boot backend.
 *
 * The frontend uses this file instead of hardcoding backend URLs
 * or implementing CSRF handling separately in every component.
 */

// -----------------------------------------------------------------------------
// 1. Backend configuration
// -----------------------------------------------------------------------------

/**
 * Base URL of our Spring Boot application.
 *
 * NEXT_PUBLIC_CORE_API_URL is configured in frontend/.env.local.
 *
 * The fallback is useful for local development, but production deployments
 * should explicitly configure their backend URL.
 */
export const API_BASE_URL =
  process.env.NEXT_PUBLIC_CORE_API_URL ?? "http://localhost:8080";

/**
 * Centralized API paths.
 *
 * Keeping paths together reduces typing mistakes and makes future API
 * version changes easier to manage.
 */
export const AUTH_API = {
  csrf: "/api/v1/auth/csrf",
  refresh: "/api/v1/auth/refresh",
  logout: "/api/v1/auth/logout",
  oauthExchange: "/api/v1/auth/oauth/exchange",
  me: "/api/v1/auth/me",
} as const;

// -----------------------------------------------------------------------------
// 2. Backend response types
// -----------------------------------------------------------------------------

/**
 * Shape returned by GET /api/v1/auth/csrf.
 *
 * The backend returns both the token and the header name expected
 * by Spring Security.
 */
export interface CsrfTokenResponse {
  token: string;
  headerName: string;
}

/**
 * Shape returned by successful login, refresh, and OAuth exchange requests.
 *
 * This matches the Java LoginResponse record in our backend.
 */
export interface LoginResponse {
  accessToken: string;
  tokenType: string;
  expiresInSeconds: number;
  userId: string;
  email: string;
}

/**
 * Shape returned by GET /api/v1/auth/me.
 */
export interface CurrentUserResponse {
  userId: string;
}

// -----------------------------------------------------------------------------
// 3. CSRF token retrieval
// -----------------------------------------------------------------------------

/**
 * Requests a CSRF token from the backend.
 *
 * Why do we need this?
 *
 * Spring Security protects state-changing requests such as POST, PUT,
 * PATCH, and DELETE against Cross-Site Request Forgery (CSRF).
 *
 * Our backend:
 *   1. Returns the CSRF token in the JSON response.
 *   2. Stores the corresponding token in the XSRF-TOKEN cookie.
 *
 * The frontend must send the returned token in the header expected
 * by Spring Security when making a protected request.
 *
 * credentials: "include" is essential because the backend also sets cookies.
 */
export async function getCsrfToken(): Promise<CsrfTokenResponse> {
  const response = await fetch(
    `${API_BASE_URL}${AUTH_API.csrf}`,
    {
      method: "GET",
      credentials: "include",
      cache: "no-store",
    }
  );

  if (!response.ok) {
    throw new Error(
      `Unable to obtain CSRF token. HTTP status: ${response.status}`
    );
  }

  const csrfResponse =
    (await response.json()) as CsrfTokenResponse;

  // Fail early if the backend response does not have the expected structure.
  if (!csrfResponse.token || !csrfResponse.headerName) {
    throw new Error("Backend returned an invalid CSRF token response.");
  }

  return csrfResponse;
}

// -----------------------------------------------------------------------------
// 4. Helper for CSRF-protected requests
// -----------------------------------------------------------------------------

/**
 * Sends a request that requires CSRF protection.
 *
 * Example:
 *
 * const response = await fetchWithCsrf(AUTH_API.logout, {
 *   method: "POST",
 * });
 *
 * This helper automatically:
 *   1. Obtains a fresh CSRF token.
 *   2. Adds that token to the expected request header.
 *   3. Includes browser cookies in the request.
 *
 * We use this for state-changing authentication operations.
 */
export async function fetchWithCsrf(
  path: string,
  options: RequestInit = {}
): Promise<Response> {
  // Obtain a CSRF token before making the protected request.
  const csrf = await getCsrfToken();

  // Headers provides a safe way to merge existing headers with our CSRF header.
  const headers = new Headers(options.headers);

  // Spring Security expects this header name and token value.
  headers.set(csrf.headerName, csrf.token);

  // Send the actual request to the backend.
  return fetch(`${API_BASE_URL}${path}`, {
    ...options,

    // Include cookies, especially the HttpOnly refresh-token cookie.
    credentials: "include",

    // Attach the CSRF token required by Spring Security.
    headers,
  });
}
