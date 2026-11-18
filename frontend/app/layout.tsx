import type React from "react";
import type { Metadata } from "next";
import { Inter } from "next/font/google";

import "./globals.css";

import { ThemeProvider } from "@/components/theme-provider";
import { AuthProvider } from "@/components/auth/auth-provider";

const inter = Inter({ subsets: ["latin"] });

export const metadata: Metadata = {
  title: "FinanceTracker - Personal Finance Management",
  description:
    "Track your income, loans, and international transfers with ease",
  icons: {
    icon: [
      {
        url: "/placeholder.svg?height=32&width=32",
        sizes: "32x32",
        type: "image/svg+xml",
      },
    ],
    apple: [
      {
        url: "/placeholder.svg?height=180&width=180",
        sizes: "180x180",
        type: "image/svg+xml",
      },
    ],
  },
  generator: "v0.dev",
};

/**
 * Root layout:
 *
 * This is a Server Component. It establishes the shared HTML document
 * and places the global providers around every route in the application.
 *
 * AuthProvider is a Client Component, but it can wrap the application
 * here so that client-side pages can consume the authentication context.
 */
export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="en" suppressHydrationWarning>
      <body className={inter.className}>
        <ThemeProvider
          attribute="class"
          defaultTheme="system"
          enableSystem
          disableTransitionOnChange
        >
          <AuthProvider>{children}</AuthProvider>
        </ThemeProvider>
      </body>
    </html>
  );
}
