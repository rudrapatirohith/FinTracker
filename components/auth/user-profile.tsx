"use client"

import { useEffect } from "react"
import { useRouter } from "next/navigation"
import { supabase } from "@/lib/supabase"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { ThemeToggle } from "@/components/theme-toggle"
import { DollarSign, ArrowRight, LogOut } from "lucide-react"
import type { User } from "@supabase/supabase-js"

interface UserProfileProps {
  user: User
}

export default function UserProfile({ user }: UserProfileProps) {
  const router = useRouter()

  // Automatically redirect to dashboard
  useEffect(() => {
    const timer = setTimeout(() => {
      router.push("/dashboard")
    }, 2000) // Redirect after 2 seconds

    return () => clearTimeout(timer)
  }, [router])

  const handleGoToDashboard = () => {
    router.push("/dashboard")
  }

  const handleSignOut = async () => {
    await supabase.auth.signOut()
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-green-50 to-emerald-100 dark:from-gray-900 dark:to-gray-800 p-4">
      <div className="w-full max-w-md">
        {/* Theme Toggle */}
        <div className="flex justify-end mb-4">
          <ThemeToggle />
        </div>

        <div className="text-center mb-8">
          <div className="flex items-center justify-center mb-4">
            <div className="bg-green-500 rounded-full p-3">
              <DollarSign className="h-8 w-8 text-white" />
            </div>
          </div>
          <h1 className="text-3xl font-bold text-gray-900 dark:text-gray-100">Welcome back!</h1>
          <p className="text-gray-600 dark:text-gray-400 mt-2">You are successfully signed in</p>
        </div>

        <Card className="border-0 shadow-xl dark:bg-gray-800/50 backdrop-blur-sm">
          <CardHeader className="text-center">
            <CardTitle className="text-green-600 dark:text-green-400">Authentication Successful</CardTitle>
            <CardDescription>Hello, {user.user_metadata?.full_name || user.email?.split("@")[0]}!</CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="bg-green-50 dark:bg-green-900/20 border border-green-200 dark:border-green-800 rounded-lg p-4">
              <p className="text-sm text-green-700 dark:text-green-300 text-center">
                Redirecting to your dashboard in a moment...
              </p>
            </div>

            <div className="space-y-3">
              <Button
                onClick={handleGoToDashboard}
                className="w-full bg-green-600 hover:bg-green-700 text-white"
                size="lg"
              >
                Go to Dashboard
                <ArrowRight className="ml-2 h-4 w-4" />
              </Button>

              <Button onClick={handleSignOut} variant="outline" className="w-full bg-transparent">
                <LogOut className="mr-2 h-4 w-4" />
                Sign Out
              </Button>
            </div>

            <div className="text-center text-sm text-gray-500 dark:text-gray-400">
              <p>Email: {user.email}</p>
              <p>Signed in at: {new Date(user.last_sign_in_at || "").toLocaleString()}</p>
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  )
}
