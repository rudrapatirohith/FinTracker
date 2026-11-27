"use client"

import { Suspense, useState } from "react"
import { useRouter, useSearchParams } from "next/navigation"
import Link from "next/link"
import { AUTH_API, fetchWithCsrf } from "@/lib/api"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Alert, AlertDescription } from "@/components/ui/alert"

function ResetPasswordForm() {
  const searchParams = useSearchParams()
  const router = useRouter()
  const token = searchParams.get("token") || ""
  const [password, setPassword] = useState("")
  const [confirmPassword, setConfirmPassword] = useState("")
  const [loading, setLoading] = useState(false)
  const [message, setMessage] = useState("")
  const [error, setError] = useState("")

  const submit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setError("")
    setMessage("")
    if (!token) {
      setError("This password reset link is missing its token. Request a new link.")
      return
    }
    if (password !== confirmPassword) {
      setError("The passwords do not match.")
      return
    }
    if (password.length < 12 || password.length > 128) {
      setError("Password must contain between 12 and 128 characters.")
      return
    }
    setLoading(true)
    try {
      const response = await fetchWithCsrf(AUTH_API.resetPassword, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ token, newPassword: password }),
      })
      if (!response.ok) {
        let detail = "This password reset link is invalid or has expired."
        try {
          const problem = await response.json()
          detail = problem.detail || problem.message || problem.title || detail
        } catch {
          // Use the safe reset-link fallback for non-JSON responses.
        }
        throw new Error(detail)
      }
      setMessage("Your password has been updated. You can now sign in.")
      setPassword("")
      setConfirmPassword("")
      setTimeout(() => router.replace("/"), 1800)
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : "Password reset failed.")
    } finally {
      setLoading(false)
    }
  }

  return (
    <main className="min-h-screen flex items-center justify-center bg-gradient-to-br from-blue-50 to-indigo-100 dark:from-gray-900 dark:to-gray-800 p-4">
      <Card className="w-full max-w-md">
        <CardHeader>
          <CardTitle>Choose a new password</CardTitle>
          <CardDescription>Use at least 12 characters. This reset link can only be used once.</CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          {message && <Alert><AlertDescription>{message}</AlertDescription></Alert>}
          {error && <Alert variant="destructive"><AlertDescription>{error}</AlertDescription></Alert>}
          <form onSubmit={submit} className="space-y-4">
            <div className="space-y-2">
              <Label htmlFor="new-password">New password</Label>
              <Input id="new-password" type="password" autoComplete="new-password" minLength={12} maxLength={128}
                required value={password} onChange={(event) => setPassword(event.target.value)} />
            </div>
            <div className="space-y-2">
              <Label htmlFor="confirm-password">Confirm password</Label>
              <Input id="confirm-password" type="password" autoComplete="new-password" minLength={12} maxLength={128}
                required value={confirmPassword} onChange={(event) => setConfirmPassword(event.target.value)} />
            </div>
            <Button type="submit" className="w-full" disabled={loading || !token}>
              {loading ? "Updating..." : "Update password"}
            </Button>
          </form>
          <div className="text-center text-sm">
            <Link href="/auth/forgot-password" className="text-primary underline-offset-4 hover:underline">Request another reset link</Link>
          </div>
        </CardContent>
      </Card>
    </main>
  )
}

export default function ResetPasswordPage() {
  return <Suspense fallback={<main className="min-h-screen flex items-center justify-center">Loading...</main>}><ResetPasswordForm /></Suspense>
}
