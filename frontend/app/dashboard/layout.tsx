"use client";

import type React from "react";
import { useEffect } from "react";
import { useRouter } from "next/navigation";

import { useAuth } from "@/components/auth/auth-provider";
import { SidebarProvider, SidebarInset } from "@/components/ui/sidebar";
import { AppSidebar } from "@/components/dashboard/app-sidebar";
import AIAssistant from "@/components/dashboard/ai-assistant";

import { Loader2 } from "lucide-react";

/**
 * Dashboard Layout
 *
 * This layout protects every route under /dashboard.
 *
 * It reads the authentication state from the global AuthProvider.
 * Unauthenticated users are redirected to the sign-in page.
 */
export default function DashboardLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const { user, isLoading } = useAuth();
  const router = useRouter();

  // Redirect only after the provider finishes restoring the session.
  useEffect(() => {
    if (!isLoading && !user) {
      router.replace("/");
    }
  }, [isLoading, user, router]);

  // Show a loading indicator while checking the backend session.
  if (isLoading) {
    return (
      <div className="flex min-h-screen items-center justify-center">
        <Loader2 className="h-8 w-8 animate-spin" />
      </div>
    );
  }

  // Do not display protected dashboard content without a user.
  if (!user) {
    return null;
  }

  return (
    <SidebarProvider>
      <div className="flex min-h-screen w-full">
        <AppSidebar user={user} />

        <SidebarInset className="flex-1">
          <div className="flex min-h-screen flex-col">
            <main className="flex-1 overflow-auto p-6">
              {children}
            </main>
          </div>
        </SidebarInset>
      </div>

      <AIAssistant />
    </SidebarProvider>
  );
}
