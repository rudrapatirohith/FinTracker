
"use client";

/**
 * Home / Sign-In Page
 *
 * Authentication is now managed by our global Spring Boot AuthProvider.
 *
 * This page has two responsibilities:
 *
 * 1. Wait for AuthProvider to restore an existing backend session.
 * 2. Redirect authenticated users to the dashboard.
 *
 * Users without an active session see the existing AuthForm.
 *
 * Important:
 * We no longer call Supabase getUser() or onAuthStateChange() here.
 */

import { useEffect } from "react";
import { useRouter } from "next/navigation";

import { useAuth } from "@/components/auth/auth-provider";
import AuthForm from "@/components/auth/auth-form";

import { Loader2 } from "lucide-react";

export default function Home() {
  const router = useRouter();

  // Read authentication state from our global backend provider.
  const { user, isLoading } = useAuth();

  /**
   * Once the backend session check finishes:
   *
   * - Authenticated users go to the dashboard.
   * - Unauthenticated users remain on the sign-in page.
   */
  useEffect(() => {
    if (!isLoading && user) {
      router.replace("/dashboard");
    }
  }, [isLoading, user, router]);

  // Do not display the sign-in form while session restoration is running.
  if (isLoading) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-gradient-to-br from-blue-50 to-indigo-100 dark:from-gray-900 dark:to-gray-800">
        <Loader2 className="h-8 w-8 animate-spin text-primary" />
      </div>
    );
  }

  // Authenticated users are being redirected to the dashboard.
  if (user) {
    return (
      <div className="flex min-h-screen items-center justify-center">
        <Loader2 className="h-8 w-8 animate-spin text-primary" />
      </div>
    );
  }

  // No backend session exists, so display the existing sign-in interface.
  return <AuthForm />;
}
