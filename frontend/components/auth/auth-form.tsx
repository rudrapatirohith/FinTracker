"use client";

/**
 * Authentication Form
 *
 * Responsibilities:
 *   1. Authenticate email/password users through Spring Boot.
 *   2. Register new users through Spring Boot.
 *   3. Start Google OAuth through the Spring Boot backend.
 *
 * Important:
 *   - This component no longer uses Supabase Auth.
 *   - AuthProvider owns the authenticated frontend session.
 *   - The backend manages refresh tokens using HttpOnly cookies.
 */

import type React from "react";

import { useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";

import {
  API_BASE_URL,
  AUTH_API,
  fetchWithCsrf,
  type ApiProblemDetail,
  type LoginResponse,
} from "@/lib/api";

import { useAuth } from "@/components/auth/auth-provider";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import {
  Tabs,
  TabsContent,
  TabsList,
  TabsTrigger,
} from "@/components/ui/tabs";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { Separator } from "@/components/ui/separator";

import {
  DollarSign,
  Mail,
  Lock,
  User,
  Eye,
  EyeOff,
  AlertCircle,
  Loader2,
} from "lucide-react";

// -----------------------------------------------------------------------------
// 1. Backend error handling
// -----------------------------------------------------------------------------

/**
 * Converts a Spring Boot ProblemDetail response into a readable message.
 *
 * Our GlobalExceptionHandler returns responses containing fields such as:
 *
 * {
 *   "title": "Authentication failed",
 *   "detail": "Invalid email or password.",
 *   "status": 401
 * }
 *
 * We prefer the detail field because it contains the actual explanation.
 */
async function getBackendErrorMessage(
  response: Response
): Promise<string> {
  try {
    const problem =
      (await response.json()) as ApiProblemDetail;

    if (problem.detail) {
      return problem.detail;
    }

    if (problem.title) {
      return problem.title;
    }
  } catch {
    // The response might not contain JSON.
    // In that case, use the HTTP status as a fallback.
  }

  return `The request failed. HTTP status: ${response.status}`;
}

// -----------------------------------------------------------------------------
// 2. Authentication form component
// -----------------------------------------------------------------------------

export default function AuthForm() {
  // ---------------------------------------------------------------------------
  // Form state
  // ---------------------------------------------------------------------------

  // Registration requires a full name.
  const [fullName, setFullName] = useState("");

  // Email is shared between the Sign In and Sign Up tabs.
  const [email, setEmail] = useState("");

  // Password is shared between both tabs.
  const [password, setPassword] = useState("");

  // Registration requires password confirmation.
  const [confirmPassword, setConfirmPassword] = useState("");

  // Controls whether the password is visible.
  const [showPassword, setShowPassword] = useState(false);

  // Controls whether the confirmation password is visible.
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);

  // Tracks email/password requests.
  const [loading, setLoading] = useState(false);

  // Tracks Google OAuth redirection.
  const [googleLoading, setGoogleLoading] = useState(false);

  // User-facing error and success messages.
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");

  // Controls which authentication tab is currently visible.
  const [activeTab, setActiveTab] = useState("signin");

  // Next.js navigation.
  const router = useRouter();

  // ---------------------------------------------------------------------------
  // Authentication provider
  // ---------------------------------------------------------------------------

  /**
   * completeLogin is provided by AuthProvider.
   *
   * It validates the backend access token through /api/v1/auth/me
   * and then stores the authenticated user and access token in React memory.
   */
  const { completeLogin } = useAuth();

  // ---------------------------------------------------------------------------
  // 3. Google authentication
  // ---------------------------------------------------------------------------

  /**
   * Starts Google authentication through our Spring Boot backend.
   *
   * We intentionally preserve the existing working Google OAuth flow.
   *
   * Spring Security handles:
   *   - Redirecting the user to Google.
   *   - OAuth Authorization Code flow.
   *   - PKCE verification.
   *   - Validating Google's identity response.
   *   - Creating our application session.
   *
   * After successful authentication, the backend redirects the browser
   * to our frontend OAuth callback page.
   */
  const handleGoogleSignIn = async () => {
    try {
      setGoogleLoading(true);
      setError("");
      setMessage("");

      // Start OAuth directly through our Java backend.
      //
      // A full browser redirect is required because OAuth navigates
      // between our application and Google's authentication page.
      window.location.assign(
        `${API_BASE_URL}/oauth2/authorization/google`
      );
    } catch (error: unknown) {
      console.error("Google sign-in error:", error);

      setError(
        error instanceof Error
          ? error.message
          : "An unexpected error occurred during Google sign-in."
      );

      setGoogleLoading(false);
    }
  };

  // ---------------------------------------------------------------------------
  // 4. Email/password login
  // ---------------------------------------------------------------------------

  /**
   * Authenticates an existing user through Spring Boot.
   *
   * Complete flow:
   *
   *   1. Send email and password to POST /api/v1/auth/login.
   *   2. Backend validates the credentials.
   *   3. Backend returns an access token in JSON.
   *   4. Backend sets the refresh token in an HttpOnly cookie.
   *   5. Pass the login response to AuthProvider.
   *   6. AuthProvider validates the access token through /me.
   *   7. Navigate to the dashboard.
   */
  const handleSignIn = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();

    setLoading(true);
    setError("");
    setMessage("");

    try {
      // Send the credentials to the Spring Boot login endpoint.
      //
      // fetchWithCsrf obtains a CSRF token, adds the expected header,
      // and includes cookies in the request.
      const response = await fetchWithCsrf(
        AUTH_API.login,
        {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
          },
          body: JSON.stringify({
            email: email.trim(),
            password,
          }),
        }
      );

      // Invalid credentials or another backend error.
      if (!response.ok) {
        throw new Error(
          await getBackendErrorMessage(response)
        );
      }

      // Read the successful backend login response.
      const loginResponse =
        (await response.json()) as LoginResponse;

      // Establish the authenticated session in our global AuthProvider.
      //
      // This also validates the access token against the backend /me endpoint.
      await completeLogin(loginResponse);

      // The dashboard is protected by AuthProvider.
      router.push("/dashboard");
    } catch (error: unknown) {
      console.error("Email/password sign-in failed:", error);

      setError(
        error instanceof Error
          ? error.message
          : "An unexpected error occurred during sign-in."
      );
    } finally {
      setLoading(false);
    }
  };

  // ---------------------------------------------------------------------------
  // 5. User registration
  // ---------------------------------------------------------------------------

  /**
   * Creates a new account through Spring Boot.
   *
   * The backend RegistrationService:
   *   - Normalizes the email.
   *   - Checks whether the email is already registered.
   *   - Hashes the password.
   *   - Creates the application user.
   *   - Creates the associated user profile.
   *   - Returns safe registration information.
   *
   * Important:
   *
   * Registration does NOT return an access token or create a login session.
   * Therefore, after successful registration, we ask the user to sign in.
   */
  const handleSignUp = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();

    setLoading(true);
    setError("");
    setMessage("");

    // Make sure both password fields contain the same value.
    if (password !== confirmPassword) {
      setError("Passwords do not match.");
      setLoading(false);
      return;
    }

    // The backend requires passwords between 12 and 128 characters.
    if (password.length < 12 || password.length > 128) {
      setError("Password must contain between 12 and 128 characters.");
      setLoading(false);
      return;
    }

    // The backend requires a non-empty full name.
    if (!fullName.trim()) {
      setError("Please enter your full name.");
      setLoading(false);
      return;
    }

    try {
      // Send registration details to the Spring Boot backend.
      const response = await fetchWithCsrf(
        AUTH_API.register,
        {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
          },
          body: JSON.stringify({
            fullName: fullName.trim(),
            email: email.trim(),
            password,
          }),
        }
      );

      // For example:
      // HTTP 409 means the email is already registered.
      // HTTP 400 means request validation failed.
      if (!response.ok) {
        throw new Error(
          await getBackendErrorMessage(response)
        );
      }

      // Registration succeeded.
      //
      // The backend returns account information, not authentication tokens.
      // We do not call completeLogin() here.
      setMessage(
        "Your account was created successfully. Please sign in with your new credentials."
      );

      // Clear password fields after successful registration.
      setPassword("");
      setConfirmPassword("");

      // Move the user to the Sign In tab.
      setActiveTab("signin");
    } catch (error: unknown) {
      console.error("Account registration failed:", error);

      setError(
        error instanceof Error
          ? error.message
          : "An unexpected error occurred during registration."
      );
    } finally {
      setLoading(false);
    }
  };

  // ---------------------------------------------------------------------------
  // 6. Tab changes
  // ---------------------------------------------------------------------------

  /**
   * Clears old messages when the user switches between Sign In and Sign Up.
   */
  const handleTabChange = (value: string) => {
    setActiveTab(value);
    setError("");
    setMessage("");
  };

  // ---------------------------------------------------------------------------
  // 7. User interface
  // ---------------------------------------------------------------------------

  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-blue-50 to-indigo-100 dark:from-gray-900 dark:to-gray-800 p-4">
      <Card className="w-full max-w-md shadow-xl">
        <CardHeader className="space-y-4 text-center">
          <div className="flex items-center justify-center gap-2">
            <div className="bg-primary rounded-lg p-2">
              <DollarSign className="h-6 w-6 text-primary-foreground" />
            </div>

            <div className="flex items-center gap-1">
              <DollarSign className="h-5 w-5 text-primary" />
              <span className="text-2xl font-bold">
                FinanceTracker
              </span>
            </div>
          </div>

          <div>
            <CardTitle className="text-2xl">
              Welcome
            </CardTitle>

            <CardDescription>
              Sign in to your account or create a new one
            </CardDescription>
          </div>
        </CardHeader>

        <CardContent className="space-y-6">

          {/* Google Sign In Button */}
          <Button
            onClick={handleGoogleSignIn}
            disabled={googleLoading || loading}
            variant="outline"
            className="w-full h-11 bg-white dark:bg-gray-800 border-gray-300 dark:border-gray-600 hover:bg-gray-50 dark:hover:bg-gray-700 text-gray-900 dark:text-gray-100 hover:text-gray-900 dark:hover:text-gray-100 transition-colors"
          >
            {googleLoading ? (
              <Loader2 className="mr-2 h-4 w-4 animate-spin" />
            ) : (
              <svg className="mr-2 h-4 w-4" viewBox="0 0 24 24">
                <path
                  fill="currentColor"
                  d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"
                />
                <path
                  fill="currentColor"
                  d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"
                />
                <path
                  fill="currentColor"
                  d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z"
                />
                <path
                  fill="currentColor"
                  d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"
                />
              </svg>
            )}

            Continue with Google
          </Button>

          <div className="relative">
            <div className="absolute inset-0 flex items-center">
              <Separator className="w-full" />
            </div>

            <div className="relative flex justify-center text-xs uppercase">
              <span className="bg-background px-2 text-muted-foreground">
                Or continue with email
              </span>
            </div>
          </div>

          {/* Error Message */}
          {error && (
            <Alert variant="destructive">
              <AlertCircle className="h-4 w-4" />
              <AlertDescription>
                {error}
              </AlertDescription>
            </Alert>
          )}

          {/* Success Message */}
          {message && (
            <Alert>
              <Mail className="h-4 w-4" />
              <AlertDescription>
                {message}
              </AlertDescription>
            </Alert>
          )}

          {/* Sign In / Sign Up Tabs */}
          <Tabs
            value={activeTab}
            onValueChange={handleTabChange}
            className="w-full"
          >
            <TabsList className="grid w-full grid-cols-2">
              <TabsTrigger value="signin">
                Sign In
              </TabsTrigger>

              <TabsTrigger value="signup">
                Sign Up
              </TabsTrigger>
            </TabsList>

            {/* ------------------------------------------------------------- */}
            {/* Sign In */}
            {/* ------------------------------------------------------------- */}

            <TabsContent value="signin" className="space-y-4">
              <form
                onSubmit={handleSignIn}
                className="space-y-4"
              >
                {/* Email */}
                <div className="space-y-2">
                  <Label htmlFor="signin-email">
                    Email
                  </Label>

                  <div className="relative">
                    <Mail className="absolute left-3 top-3 h-4 w-4 text-muted-foreground" />

                    <Input
                      id="signin-email"
                      type="email"
                      placeholder="Enter your email"
                      value={email}
                      onChange={(e) => setEmail(e.target.value)}
                      className="pl-10"
                      required
                      disabled={loading || googleLoading}
                    />
                  </div>
                </div>

                {/* Password */}
                <div className="space-y-2">
                  <Label htmlFor="signin-password">
                    Password
                  </Label>

                  <div className="relative">
                    <Lock className="absolute left-3 top-3 h-4 w-4 text-muted-foreground" />

                    <Input
                      id="signin-password"
                      type={showPassword ? "text" : "password"}
                      placeholder="Enter your password"
                      value={password}
                      onChange={(e) => setPassword(e.target.value)}
                      className="pl-10 pr-10"
                      required
                      disabled={loading || googleLoading}
                    />

                    <Button
                      type="button"
                      variant="ghost"
                      size="sm"
                      className="absolute right-0 top-0 h-full px-3 py-2 hover:bg-transparent"
                      onClick={() => setShowPassword(!showPassword)}
                      disabled={loading || googleLoading}
                    >
                      {showPassword ? (
                        <EyeOff className="h-4 w-4 text-muted-foreground" />
                      ) : (
                        <Eye className="h-4 w-4 text-muted-foreground" />
                      )}
                    </Button>
                  </div>
                </div>

                <div className="flex justify-end">
                  <Link
                    href="/auth/forgot-password"
                    className="text-sm text-primary underline-offset-4 hover:underline"
                  >
                    Forgot password?
                  </Link>
                </div>

                {/* Submit */}
                <Button
                  type="submit"
                  className="w-full"
                  disabled={loading || googleLoading}
                >
                  {loading ? (
                    <>
                      <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                      Signing in...
                    </>
                  ) : (
                    "Sign In"
                  )}
                </Button>
              </form>
            </TabsContent>

            {/* ------------------------------------------------------------- */}
            {/* Sign Up */}
            {/* ------------------------------------------------------------- */}

            <TabsContent value="signup" className="space-y-4">
              <form
                onSubmit={handleSignUp}
                className="space-y-4"
              >
                {/* Full Name */}
                <div className="space-y-2">
                  <Label htmlFor="signup-full-name">
                    Full Name
                  </Label>

                  <div className="relative">
                    <User className="absolute left-3 top-3 h-4 w-4 text-muted-foreground" />

                    <Input
                      id="signup-full-name"
                      type="text"
                      placeholder="Enter your full name"
                      value={fullName}
                      onChange={(e) => setFullName(e.target.value)}
                      className="pl-10"
                      maxLength={200}
                      required
                      disabled={loading || googleLoading}
                    />
                  </div>
                </div>

                {/* Email */}
                <div className="space-y-2">
                  <Label htmlFor="signup-email">
                    Email
                  </Label>

                  <div className="relative">
                    <Mail className="absolute left-3 top-3 h-4 w-4 text-muted-foreground" />

                    <Input
                      id="signup-email"
                      type="email"
                      placeholder="Enter your email"
                      value={email}
                      onChange={(e) => setEmail(e.target.value)}
                      className="pl-10"
                      required
                      disabled={loading || googleLoading}
                    />
                  </div>
                </div>

                {/* Password */}
                <div className="space-y-2">
                  <Label htmlFor="signup-password">
                    Password
                  </Label>

                  <div className="relative">
                    <Lock className="absolute left-3 top-3 h-4 w-4 text-muted-foreground" />

                    <Input
                      id="signup-password"
                      type={showPassword ? "text" : "password"}
                      placeholder="Create a password"
                      value={password}
                      onChange={(e) => setPassword(e.target.value)}
                      className="pl-10 pr-10"
                      minLength={12}
                      maxLength={128}
                      required
                      disabled={loading || googleLoading}
                    />

                    <Button
                      type="button"
                      variant="ghost"
                      size="sm"
                      className="absolute right-0 top-0 h-full px-3 py-2 hover:bg-transparent"
                      onClick={() => setShowPassword(!showPassword)}
                      disabled={loading || googleLoading}
                    >
                      {showPassword ? (
                        <EyeOff className="h-4 w-4 text-muted-foreground" />
                      ) : (
                        <Eye className="h-4 w-4 text-muted-foreground" />
                      )}
                    </Button>
                  </div>

                  <p className="text-xs text-muted-foreground">
                    Use between 12 and 128 characters.
                  </p>
                </div>

                {/* Confirm Password */}
                <div className="space-y-2">
                  <Label htmlFor="confirm-password">
                    Confirm Password
                  </Label>

                  <div className="relative">
                    <Lock className="absolute left-3 top-3 h-4 w-4 text-muted-foreground" />

                    <Input
                      id="confirm-password"
                      type={showConfirmPassword ? "text" : "password"}
                      placeholder="Confirm your password"
                      value={confirmPassword}
                      onChange={(e) => setConfirmPassword(e.target.value)}
                      className="pl-10 pr-10"
                      required
                      disabled={loading || googleLoading}
                    />

                    <Button
                      type="button"
                      variant="ghost"
                      size="sm"
                      className="absolute right-0 top-0 h-full px-3 py-2 hover:bg-transparent"
                      onClick={() =>
                        setShowConfirmPassword(!showConfirmPassword)
                      }
                      disabled={loading || googleLoading}
                    >
                      {showConfirmPassword ? (
                        <EyeOff className="h-4 w-4 text-muted-foreground" />
                      ) : (
                        <Eye className="h-4 w-4 text-muted-foreground" />
                      )}
                    </Button>
                  </div>
                </div>

                {/* Submit */}
                <Button
                  type="submit"
                  className="w-full"
                  disabled={loading || googleLoading}
                >
                  {loading ? (
                    <>
                      <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                      Creating account...
                    </>
                  ) : (
                    "Create Account"
                  )}
                </Button>
              </form>
            </TabsContent>
          </Tabs>
        </CardContent>
      </Card>
    </div>
  );
}
