"use client";

/**
 * AuthProvider
 *
 * Owns the frontend authentication state.
 *
 * It makes the following available to components inside the provider:
 *   - The currently authenticated user.
 *   - The current short-lived access token.
 *   - A method to complete login.
 *   - A method to sign out.
 *
 * Security design:
 *   - The access token stays in React memory.
 *   - The long-lived refresh token stays in the backend's HttpOnly cookie.
 *   - The frontend never reads the refresh token directly.
 */

import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useRef,
  useState,
  type ReactNode,
} from "react";

import { usePathname } from "next/navigation";

import {
  API_BASE_URL,
  AUTH_API,
  fetchWithCsrf,
  registerAccessTokenRefreshListener,
  type CurrentUserResponse,
  type LoginResponse,
} from "@/lib/api";

// -----------------------------------------------------------------------------
// 1. Public authentication types
// -----------------------------------------------------------------------------

/**
 * User information exposed to frontend components.
 *
 * Our current backend account model stores an email address.
 * We derive a temporary display name from the email until
 * we implement the separate user-profile feature.
 */
export interface AuthUser {
  id: string;
  email: string;
  displayName: string;
  avatarUrl: string;
}

/**
 * Values and operations available through useAuth().
 */
interface AuthContextValue {
  user: AuthUser | null;
  accessToken: string | null;
  isLoading: boolean;

  completeLogin: (loginResponse: LoginResponse) => Promise<void>;
  signOut: () => Promise<void>;
}

// -----------------------------------------------------------------------------
// 2. Context creation
// -----------------------------------------------------------------------------

/**
 * React Context allows components to access shared authentication state
 * without passing user and token props through every component.
 *
 * null means that a component is not currently connected to the provider.
 */
const AuthContext = createContext<AuthContextValue | null>(null);

// -----------------------------------------------------------------------------
// 3. Convert backend response into frontend user information
// -----------------------------------------------------------------------------

/**
 * The backend currently returns userId and email.
 *
 * We derive a display name from the email prefix and use a placeholder avatar.
 */
function createAuthUser(loginResponse: LoginResponse): AuthUser {
  const emailPrefix = loginResponse.email.split("@")[0];

  return {
    id: loginResponse.userId,
    email: loginResponse.email,
    displayName: emailPrefix || "User",
    avatarUrl: "/placeholder.svg",
  };
}

// -----------------------------------------------------------------------------
// 4. Provider component
// -----------------------------------------------------------------------------

interface AuthProviderProps {
  children: ReactNode;
}

export function AuthProvider({ children }: AuthProviderProps) {
  const pathname = usePathname();

  // The authenticated user currently known to the frontend.
  const [user, setUser] = useState<AuthUser | null>(null);

  // Short-lived JWT access token. Never persist this in localStorage.
  const [accessToken, setAccessToken] = useState<string | null>(null);

  // True while the provider is checking whether a refresh session exists.
  const [isLoading, setIsLoading] = useState(true);

  /**
   * Keep the in-memory React session synchronized when the shared API client
   * rotates an expired access token after a protected request receives 401.
   */
  useEffect(() => {
    return registerAccessTokenRefreshListener((session) => {
      if (!session) {
        sessionEstablishedRef.current = false;
        setUser(null);
        setAccessToken(null);
        return;
      }

      setUser(createAuthUser(session));
      setAccessToken(session.accessToken);
      sessionEstablishedRef.current = true;
    });
  }, []);

  /**
   * Tracks whether this provider instance has already established
   * an authenticated session.
   *
   * This is a ref because the value controls an effect, but changing it
   * does not need to trigger a React render.
   *
   * It prevents a second refresh request after:
   *   - A successful Google OAuth callback.
   *   - A successful session restoration.
   */
  const sessionEstablishedRef = useRef(false);

  /**
   * Stores the session-restoration request.
   *
   * React Strict Mode can run effects more than once in development.
   * Reusing this promise prevents duplicate refresh requests.
   */
  const restorationPromiseRef = useRef<Promise<void> | null>(null);

  // ---------------------------------------------------------------------------
  // 5. Establish and validate an authenticated session
  // ---------------------------------------------------------------------------

  /**
   * Accepts a successful backend login, refresh, or OAuth response.
   *
   * Before trusting the response, we call GET /me with the access token.
   * This confirms that the backend accepts the token and that the
   * authenticated user ID matches the login response.
   */
  const completeLogin = useCallback(
    async (loginResponse: LoginResponse): Promise<void> => {
      if (
        !loginResponse.accessToken ||
        !loginResponse.userId ||
        !loginResponse.email
      ) {
        throw new Error(
          "Backend returned incomplete login information."
        );
      }

      const response = await fetch(
        `${API_BASE_URL}${AUTH_API.me}`,
        {
          method: "GET",
          credentials: "include",
          cache: "no-store",
          headers: {
            Authorization: `Bearer ${loginResponse.accessToken}`,
          },
        }
      );

      if (!response.ok) {
        throw new Error(
          `Unable to validate authenticated user. HTTP status: ${response.status}`
        );
      }

      const currentUser =
        (await response.json()) as CurrentUserResponse;

      // Prevent accepting inconsistent identity information.
      if (currentUser.userId !== loginResponse.userId) {
        throw new Error(
          "Authenticated user ID does not match the login response."
        );
      }

      // Store the validated user and access token in React memory.
      setUser(createAuthUser(loginResponse));
      setAccessToken(loginResponse.accessToken);

      /**
       * Mark the session as established.
       *
       * This is important after Google OAuth: when the callback navigates
       * to /dashboard, the provider must not immediately refresh again.
       */
      sessionEstablishedRef.current = true;
    },
    []
  );

  // ---------------------------------------------------------------------------
  // 6. Restore an existing session after a page reload
  // ---------------------------------------------------------------------------

  /**
   * The access token exists only in memory.
   *
   * After a browser reload:
   *   1. The React access token is gone.
   *   2. The browser still has the HttpOnly refresh-token cookie.
   *   3. We ask the backend to rotate the refresh token.
   *   4. The backend returns a new access token.
   *   5. We validate that token using /me.
   */
  useEffect(() => {
    /**
     * Do not refresh on the Google OAuth callback route.
     *
     * The callback must exchange its one-time handoff code using
     * the refresh session created during Google authentication.
     */
    if (pathname === "/auth/callback") {
      setIsLoading(false);
      return;
    }

    /**
     * A session was already established by login or restoration.
     *
     * In particular, this prevents a second refresh immediately after
     * the OAuth callback navigates to /dashboard.
     */
    if (sessionEstablishedRef.current) {
      return;
    }

    /**
     * Reuse an existing restoration promise if this effect runs again.
     * This prevents duplicate refresh requests in development.
     */
    if (!restorationPromiseRef.current) {
      restorationPromiseRef.current = (async () => {
        try {
          // Ask the backend to rotate the existing refresh session.
          const response = await fetchWithCsrf(
            AUTH_API.refresh,
            {
              method: "POST",
            }
          );

          // No valid refresh session means the user is not authenticated.
          if (!response.ok) {
            sessionEstablishedRef.current = false;
            setUser(null);
            setAccessToken(null);
            return;
          }

          const loginResponse =
            (await response.json()) as LoginResponse;

          // Validate the new access token before accepting the session.
          await completeLogin(loginResponse);
        } catch (error) {
          // Do not leave stale authentication state after failed restoration.
          console.error("Session restoration failed:", error);

          sessionEstablishedRef.current = false;
          setUser(null);
          setAccessToken(null);
        } finally {
          setIsLoading(false);
        }
      })();
    }

    void restorationPromiseRef.current;
  }, [pathname, completeLogin]);

  // ---------------------------------------------------------------------------
  // 7. Sign out
  // ---------------------------------------------------------------------------

  /**
   * Calls the backend logout endpoint to invalidate the refresh session.
   *
   * Local authentication state is cleared even if the request fails.
   */
  const signOut = useCallback(async (): Promise<void> => {
    try {
      const response = await fetchWithCsrf(
        AUTH_API.logout,
        {
          method: "POST",
        }
      );

      if (!response.ok) {
        throw new Error(
          `Backend logout failed. HTTP status: ${response.status}`
        );
      }
    } finally {
      // Mark the session as no longer established.
      sessionEstablishedRef.current = false;

      // Remove the in-memory session regardless of request outcome.
      setUser(null);
      setAccessToken(null);
    }
  }, []);

  // ---------------------------------------------------------------------------
  // 8. Share authentication state with child components
  // ---------------------------------------------------------------------------

  /**
   * useMemo avoids creating a new context value on every unrelated render.
   */
  const contextValue = useMemo<AuthContextValue>(
    () => ({
      user,
      accessToken,
      isLoading,
      completeLogin,
      signOut,
    }),
    [user, accessToken, isLoading, completeLogin, signOut]
  );

  return (
    <AuthContext.Provider value={contextValue}>
      {children}
    </AuthContext.Provider>
  );
}

// -----------------------------------------------------------------------------
// 9. Consumer hook
// -----------------------------------------------------------------------------

/**
 * Components call useAuth() to access shared authentication state.
 *
 * Example:
 * const { user, accessToken, isLoading, signOut } = useAuth();
 *
 * The check catches a common configuration mistake:
 * using the hook outside AuthProvider.
 */
export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext);

  if (!context) {
    throw new Error(
      "useAuth must be used inside an AuthProvider."
    );
  }

  return context;
}
