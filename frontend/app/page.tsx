"use client"

import { useEffect, useState } from "react"
import { supabase } from "@/lib/supabase"
import AuthForm from "@/components/auth/auth-form"
import UserProfile from "@/components/auth/user-profile"
import { useRouter } from "next/navigation"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { AlertTriangle } from "lucide-react"
import type { User } from "@supabase/supabase-js"

export default function Home() {
  const [user, setUser] = useState<User | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const router = useRouter()

  useEffect(() => {
    const getUser = async () => {
      try {
        const {
          data: { user },
        } = await supabase.auth.getUser()
        setUser(user)

        // If user is already authenticated, redirect to dashboard
        if (user) {
          router.push("/dashboard")
        }
      } catch (err) {
        console.error("Auth error:", err)
        setError(err instanceof Error ? err.message : "Authentication failed")
      } finally {
        setLoading(false)
      }
    }

    getUser()

    const {
      data: { subscription },
    } = supabase.auth.onAuthStateChange((event, session) => {
      setUser(session?.user ?? null)
      if (session?.user) {
        // Redirect to dashboard on successful authentication
        router.push("/dashboard")
      }
    })

    return () => subscription.unsubscribe()
  }, [router])

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-screen bg-gradient-to-br from-blue-50 to-indigo-100 dark:from-gray-900 dark:to-gray-800">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary"></div>
      </div>
    )
  }

  if (error) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-red-50 to-orange-100 dark:from-gray-900 dark:to-gray-800 p-4">
        <Card className="w-full max-w-md">
          <CardHeader className="text-center">
            <div className="mx-auto bg-red-100 rounded-full p-3 w-fit mb-4">
              <AlertTriangle className="h-8 w-8 text-red-600" />
            </div>
            <CardTitle className="text-red-900 dark:text-red-400">Configuration Error</CardTitle>
            <CardDescription>Please check your environment variables</CardDescription>
          </CardHeader>
          <CardContent>
            <div className="bg-yellow-50 dark:bg-yellow-900/20 border border-yellow-200 dark:border-yellow-800 rounded-lg p-4">
              <p className="text-sm text-yellow-700 dark:text-yellow-300 mb-3">
                Make sure you have set up your{" "}
                <code className="bg-yellow-100 dark:bg-yellow-800 px-1 rounded">.env.local</code> file with:
              </p>
              <div className="bg-gray-50 dark:bg-gray-800 border rounded-lg p-3">
                <p className="text-xs text-gray-600 dark:text-gray-400 font-mono">
                  NEXT_PUBLIC_SUPABASE_URL=https://your-project.supabase.co
                  <br />
                  NEXT_PUBLIC_SUPABASE_ANON_KEY=your-anon-key
                </p>
              </div>
            </div>
          </CardContent>
        </Card>
      </div>
    )
  }

  // If user exists, show profile (which will redirect to dashboard)
  if (user) {
    return <UserProfile user={user} />
  }

  // Show auth form if no user
  return <AuthForm />
}
