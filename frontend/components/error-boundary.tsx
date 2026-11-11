"use client"

import { useEffect } from "react"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { AlertTriangle, RefreshCw } from "lucide-react"

interface ErrorBoundaryProps {
  error: Error & { digest?: string }
  reset: () => void
}

export default function ErrorBoundary({ error, reset }: ErrorBoundaryProps) {
  useEffect(() => {
    console.error("Application error:", error)
  }, [error])

  const isSupabaseConfigError = error.message.includes("NEXT_PUBLIC_SUPABASE")

  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-red-50 to-orange-100 p-4">
      <Card className="w-full max-w-md">
        <CardHeader className="text-center">
          <div className="mx-auto bg-red-100 rounded-full p-3 w-fit mb-4">
            <AlertTriangle className="h-8 w-8 text-red-600" />
          </div>
          <CardTitle className="text-red-900">
            {isSupabaseConfigError ? "Configuration Error" : "Something went wrong"}
          </CardTitle>
          <CardDescription>
            {isSupabaseConfigError
              ? "The application is not properly configured."
              : "An unexpected error occurred while loading the application."}
          </CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          {isSupabaseConfigError ? (
            <div className="space-y-3">
              <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-4">
                <h4 className="font-medium text-yellow-800 mb-2">Missing Environment Variables</h4>
                <p className="text-sm text-yellow-700 mb-3">Please set up your Supabase configuration:</p>
                <ol className="text-sm text-yellow-700 space-y-1 list-decimal list-inside">
                  <li>
                    Create a <code className="bg-yellow-100 px-1 rounded">.env.local</code> file
                  </li>
                  <li>Add your Supabase URL and API key</li>
                  <li>Restart the development server</li>
                </ol>
              </div>
              <div className="bg-gray-50 border rounded-lg p-3">
                <p className="text-xs text-gray-600 font-mono">
                  NEXT_PUBLIC_SUPABASE_URL=https://your-project.supabase.co
                  <br />
                  NEXT_PUBLIC_SUPABASE_ANON_KEY=your-anon-key
                </p>
              </div>
            </div>
          ) : (
            <div className="bg-gray-50 border rounded-lg p-4">
              <p className="text-sm text-gray-600 mb-2">Error details:</p>
              <code className="text-xs text-red-600 bg-red-50 p-2 rounded block">{error.message}</code>
            </div>
          )}

          <Button onClick={reset} className="w-full">
            <RefreshCw className="mr-2 h-4 w-4" />
            Try Again
          </Button>
        </CardContent>
      </Card>
    </div>
  )
}
