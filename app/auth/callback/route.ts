import { createClient } from "@/lib/supabase-server"
import { type NextRequest, NextResponse } from "next/server"

export async function GET(request: NextRequest) {
  const { searchParams, origin } = new URL(request.url)
  const code = searchParams.get("code")
  const next = searchParams.get("next") ?? "/dashboard"

  if (code) {
    try {
      const supabase = await createClient()
      const { error } = await supabase.auth.exchangeCodeForSession(code)
      if (!error) {
        return NextResponse.redirect(`${origin}${next}`)
      }
    } catch (error) {
      console.error("Supabase client error:", error)
      return NextResponse.redirect(`${origin}/auth/auth-code-error`)
    }
  }

  return NextResponse.redirect(`${origin}/auth/auth-code-error`)
}
