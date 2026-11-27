"use client"

import { useState } from "react"
import Link from "next/link"
import { AUTH_API, fetchWithCsrf } from "@/lib/api"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Alert, AlertDescription } from "@/components/ui/alert"

export default function ForgotPasswordPage() {
  const [email, setEmail] = useState("")
  const [loading, setLoading] = useState(false)
  const [message, setMessage] = useState("")
  const [error, setError] = useState("")

  const submit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setLoading(true)
    setMessage("")
    setError("")
    try {
      const response = await fetchWithCsrf(AUTH_API.forgotPassword, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email: email.trim() }),
      })
      if (!response.ok) {
        let detail = "Password recovery could not be started."
        try {
          const problem = await response.json()
          detail = problem.detail || problem.message || problem.title || detail
        } catch {
          // Keep the fallback for non-JSON error responses.
        }
        throw new Error(detail)
      }
      setMessage("If an account exists for this email, a reset link will be sent shortly. Check your inbox.")
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : "Password recovery could not be started.")
    } finally {
      setLoading(false)
    }
  }

  return (
    <main className="min-h-screen flex items-center justify-center bg-gradient-to-br from-blue-50 to-indigo-100 dark:from-gray-900 dark:to-gray-800 p-4">
      <Card className="w-full max-w-md">
        <CardHeader>
          <CardTitle>Reset your password</CardTitle>
          <CardDescription>Enter the email address associated with your FinTracker account.</CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          {message && <Alert><AlertDescription>{message}</AlertDescription></Alert>}
          {error && <Alert variant="destructive"><AlertDescription>{error}</AlertDescription></Alert>}
          <form onSubmit={submit} className="space-y-4">
            <div className="space-y-2">
              <Label htmlFor="recovery-email">Email</Label>
              <Input id="recovery-email" type="email" autoComplete="email" required maxLength={320}
                value={email} onChange={(event) => setEmail(event.target.value)} />
            </div>
            <Button type="submit" className="w-full" disabled={loading}>
              {loading ? "Sending..." : "Send reset link"}
            </Button>
          </form>
          <div className="text-center text-sm">
            <Link href="/" className="text-primary underline-offset-4 hover:underline">Back to sign in</Link>
          </div>
        </CardContent>
      </Card>
    </main>
  )
}
