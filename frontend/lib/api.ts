/**
 * Typed transport for the Spring Boot API.
 *
 * Authentication is deliberately supplied by the caller: the access token lives
 * in AuthProvider memory and is never copied into localStorage or a cookie.
 * The backend derives record ownership from that token, so request DTOs must
 * never contain a client-selected userId.
 */

export const API_BASE_URL =
  process.env.NEXT_PUBLIC_CORE_API_URL ?? "http://localhost:8080";

export const AUTH_API = {
  csrf: "/api/v1/auth/csrf",
  register: "/api/v1/auth/register",
  login: "/api/v1/auth/login",
  refresh: "/api/v1/auth/refresh",
  logout: "/api/v1/auth/logout",
  oauthExchange: "/api/v1/auth/oauth/exchange",
  forgotPassword: "/api/v1/auth/password/forgot",
  resetPassword: "/api/v1/auth/password/reset",
  me: "/api/v1/auth/me",
} as const;

export const API = {
  income: "/api/v1/income",
  monthlyExpenses: "/api/v1/monthly-expenses",
  loans: "/api/v1/loans",
  transfers: "/api/v1/international-transfers",
  scheduledPayments: "/api/v1/scheduled-payments",
  dashboardSummary: "/api/v1/dashboard/summary",
  analyticsCategories: "/api/v1/analytics/categories",
  analyticsMonthlyTrends: "/api/v1/analytics/monthly-trends",
  analyticsWeeklyTrends: "/api/v1/analytics/weekly-trends",
  reportsSummary: "/api/v1/reports/summary",
  profile: "/api/v1/profile",
  aiChat: "/api/v1/ai/chat",
} as const;

export interface CsrfTokenResponse {
  token: string;
  headerName: string;
}

export interface LoginResponse {
  accessToken: string;
  tokenType: string;
  expiresInSeconds: number;
  userId: string;
  email: string;
}

export interface RegisterResponse {
  id: string;
  email: string;
  fullName: string;
  status: string;
}

export interface CurrentUserResponse {
  userId: string;
}

export interface ApiPage<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  first: boolean;
  last: boolean;
  numberOfElements: number;
  empty: boolean;
}

export interface ApiProblemDetail {
  title?: string;
  detail?: string;
  status?: number;
  message?: string;
}

export class ApiError extends Error {
  readonly status: number;
  readonly title?: string;

  constructor(message: string, status: number, title?: string) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.title = title;
  }
}

export async function getCsrfToken(): Promise<CsrfTokenResponse> {
  const response = await fetch(`${API_BASE_URL}${AUTH_API.csrf}`, {
    method: "GET",
    credentials: "include",
    cache: "no-store",
  });

  if (!response.ok) {
    throw new ApiError(
      `Unable to obtain CSRF token (HTTP ${response.status}).`,
      response.status
    );
  }

  const csrf = (await response.json()) as CsrfTokenResponse;
  if (!csrf.token || !csrf.headerName) {
    throw new Error("Backend returned an invalid CSRF token response.");
  }
  return csrf;
}

/**
 * Authentication endpoints use cookie-backed refresh sessions. This helper
 * obtains the CSRF token for each state-changing request and preserves caller
 * headers so OAuth/login payloads can share the same transport.
 */
export async function fetchWithCsrf(
  path: string,
  options: RequestInit = {}
): Promise<Response> {
  const csrf = await getCsrfToken();
  const headers = new Headers(options.headers);
  headers.set(csrf.headerName, csrf.token);

  return fetch(`${API_BASE_URL}${path}`, {
    ...options,
    credentials: "include",
    headers,
  });
}

const MUTATING_METHODS = new Set(["POST", "PUT", "PATCH", "DELETE"]);

let accessTokenRefreshListener: ((session: LoginResponse | null) => void) | null = null;
let refreshInFlight: Promise<LoginResponse | null> | null = null;

/**
 * AuthProvider registers the state update callback. This keeps token refresh
 * centralized without persisting access tokens outside React memory.
 */
export function registerAccessTokenRefreshListener(
  listener: (session: LoginResponse | null) => void
): () => void {
  accessTokenRefreshListener = listener;
  return () => {
    if (accessTokenRefreshListener === listener) {
      accessTokenRefreshListener = null;
    }
  };
}

async function refreshSession(): Promise<LoginResponse | null> {
  if (!refreshInFlight) {
    refreshInFlight = (async () => {
      try {
        const response = await fetchWithCsrf(AUTH_API.refresh, { method: "POST" });
        if (!response.ok) return null;

        const session = (await response.json()) as LoginResponse;
        if (!session.accessToken || !session.userId || !session.email) return null;

        // Validate the rotated token before exposing it to React or retrying a
        // financial request. The refresh cookie itself remains HttpOnly.
        const validation = await fetch(`${API_BASE_URL}${AUTH_API.me}`, {
          method: "GET",
          credentials: "include",
          cache: "no-store",
          headers: { Authorization: `Bearer ${session.accessToken}` },
        });
        if (!validation.ok) return null;

        const currentUser = (await validation.json()) as CurrentUserResponse;
        return currentUser.userId === session.userId ? session : null;
      } catch (error) {
        console.error("Access-token refresh failed:", error);
        return null;
      }
    })().finally(() => {
      refreshInFlight = null;
    });
  }
  return refreshInFlight;
}

/**
 * Sends an authenticated API request. Spring Security's CSRF token is attached
 * to mutations in addition to the short-lived bearer token. The server remains
 * authoritative for ownership; callers pass only business fields and record IDs.
 */
export async function apiRequest<T>(
  path: string,
  accessToken: string,
  options: RequestInit = {}
): Promise<T> {
  if (!accessToken) {
    throw new ApiError("Your session has expired. Please sign in again.", 401);
  }

  const method = (options.method ?? "GET").toUpperCase();
  const headers = new Headers(options.headers);
  headers.set("Accept", "application/json");
  headers.set("Authorization", `Bearer ${accessToken}`);

  if (options.body !== undefined && !headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json");
  }

  const requestOptions: RequestInit = {
    ...options,
    method,
    headers,
    credentials: "include",
    cache: "no-store",
  };

  const execute = (token: string) => {
    const retryHeaders = new Headers(requestOptions.headers);
    retryHeaders.set("Authorization", `Bearer ${token}`);
    const retryOptions = { ...requestOptions, headers: retryHeaders };
    return MUTATING_METHODS.has(method)
      ? fetchWithCsrf(path, retryOptions)
      : fetch(`${API_BASE_URL}${path}`, retryOptions);
  };

  let activeToken = accessToken;
  let response = await execute(activeToken);

  // Access tokens are intentionally short-lived. On expiry, rotate the
  // HttpOnly refresh session once, update AuthProvider memory, and retry the
  // original request once. A shared promise prevents parallel 401s from
  // racing the backend's one-time refresh-token rotation.
  if (response.status === 401 && !path.startsWith("/api/v1/auth/")) {
    const refreshedSession = await refreshSession();
    if (!refreshedSession) {
      accessTokenRefreshListener?.(null);
    } else {
      activeToken = refreshedSession.accessToken;
      accessTokenRefreshListener?.(refreshedSession);
      response = await execute(activeToken);
    }
  }

  if (!response.ok) {
    let problem: ApiProblemDetail | undefined;
    try {
      problem = (await response.json()) as ApiProblemDetail;
    } catch {
      // Some expected responses, such as 401/403, intentionally have no body.
    }

    throw new ApiError(
      problem?.detail ??
        problem?.message ??
        problem?.title ??
        `Request failed (HTTP ${response.status}).`,
      response.status,
      problem?.title
    );
  }

  if (response.status === 204) {
    return undefined as T;
  }

  return (await response.json()) as T;
}

/**
 * Spring Data returns collections in a Page envelope. Normalizing that shape
 * here keeps screen components independent of pagination metadata when they
 * intentionally request the first page only.
 */
export async function apiGetPage<T>(
  path: string,
  accessToken: string
): Promise<ApiPage<T>> {
  return apiRequest<ApiPage<T>>(path, accessToken);
}
