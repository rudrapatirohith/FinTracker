import { createClient } from "@supabase/supabase-js"

// Validate environment variables
const supabaseUrl = process.env.NEXT_PUBLIC_SUPABASE_URL
const supabaseAnonKey = process.env.NEXT_PUBLIC_SUPABASE_ANON_KEY

if (!supabaseUrl) {
  throw new Error("Missing environment variable: NEXT_PUBLIC_SUPABASE_URL")
}

if (!supabaseAnonKey) {
  throw new Error("Missing environment variable: NEXT_PUBLIC_SUPABASE_ANON_KEY")
}

// Validate URL format
try {
  new URL(supabaseUrl)
} catch (error) {
  throw new Error(`Invalid NEXT_PUBLIC_SUPABASE_URL format: ${supabaseUrl}`)
}

// Create Supabase client
export const supabase = createClient(supabaseUrl, supabaseAnonKey, {
  auth: {
    autoRefreshToken: true,
    persistSession: true,
    detectSessionInUrl: true,
  },
  global: {
    headers: {
      "X-Client-Info": "finance-tracker",
    },
  },
})

// Error handling helper
export function handleSupabaseError(error: any, operation: string) {
  console.error(`Supabase error during ${operation}:`, error)

  // Handle specific error types
  if (error.code === "PGRST116") {
    throw new Error(`Table not found. Please run the database setup script.`)
  }

  if (error.code === "23505") {
    throw new Error(`Duplicate entry. This record already exists.`)
  }

  if (error.code === "23503") {
    throw new Error(`Invalid reference. Please check your data.`)
  }

  if (error.code === "42P01") {
    throw new Error(`Database table missing. Please run the setup script.`)
  }

  if (error.message?.includes("JWT")) {
    throw new Error(`Authentication error. Please log in again.`)
  }

  if (error.message?.includes("RLS")) {
    throw new Error(`Permission denied. Please check your access rights.`)
  }

  // Generic error
  throw new Error(error.message || `Failed to ${operation}`)
}

// Helper to check if user is authenticated
export async function requireAuth() {
  const {
    data: { user },
    error,
  } = await supabase.auth.getUser()

  if (error) {
    throw new Error("Authentication error")
  }

  if (!user) {
    throw new Error("Please log in to continue")
  }

  return user
}

// Test connection function
export async function testSupabaseConnection() {
  try {
    const { data, error } = await supabase.from("profiles").select("count").limit(1)
    if (error) {
      handleSupabaseError(error, "test connection")
    }
    return { success: true, message: "Connection successful" }
  } catch (error: any) {
    return { success: false, message: error.message }
  }
}

export type Database = {
  public: {
    Tables: {
      profiles: {
        Row: {
          id: string
          email: string | null
          full_name: string | null
          avatar_url: string | null
          currency_preference: string | null
          timezone: string | null
          created_at: string
          updated_at: string
        }
        Insert: {
          id: string
          email?: string | null
          full_name?: string | null
          avatar_url?: string | null
          currency_preference?: string | null
          timezone?: string | null
        }
        Update: {
          email?: string | null
          full_name?: string | null
          avatar_url?: string | null
          currency_preference?: string | null
          timezone?: string | null
        }
      }
      income: {
        Row: {
          id: string
          user_id: string
          source: string
          amount: number
          currency: string | null
          category: string | null
          description: string | null
          date: string
          is_recurring: boolean | null
          recurring_frequency: string | null
          created_at: string
          updated_at: string
        }
        Insert: {
          user_id: string
          source: string
          amount: number
          currency?: string | null
          category?: string | null
          description?: string | null
          date: string
          is_recurring?: boolean | null
          recurring_frequency?: string | null
        }
        Update: {
          source?: string
          amount?: number
          currency?: string | null
          category?: string | null
          description?: string | null
          date?: string
          is_recurring?: boolean | null
          recurring_frequency?: string | null
        }
      }
      loans: {
        Row: {
          id: string
          user_id: string
          loan_name: string
          principal_amount: number
          current_balance: number
          interest_rate: number | null
          monthly_payment: number | null
          start_date: string
          end_date: string | null
          lender: string | null
          loan_type: string | null
          status: string | null
          created_at: string
          updated_at: string
        }
        Insert: {
          user_id: string
          loan_name: string
          principal_amount: number
          current_balance: number
          interest_rate?: number | null
          monthly_payment?: number | null
          start_date: string
          end_date?: string | null
          lender?: string | null
          loan_type?: string | null
          status?: string | null
        }
        Update: {
          loan_name?: string
          principal_amount?: number
          current_balance?: number
          interest_rate?: number | null
          monthly_payment?: number | null
          start_date?: string
          end_date?: string | null
          lender?: string | null
          loan_type?: string | null
          status?: string | null
        }
      }
      international_transfers: {
        Row: {
          id: string
          user_id: string
          recipient_name: string
          recipient_country: string
          amount_sent: number
          currency_sent: string
          amount_received: number
          currency_received: string
          exchange_rate: number
          transfer_fee: number
          transfer_method: string | null
          purpose: string | null
          status: string
          transfer_date: string
          completion_date: string | null
          reference_number: string | null
          created_at: string
          updated_at: string
        }
        Insert: {
          user_id: string
          recipient_name: string
          recipient_country: string
          amount_sent: number
          currency_sent?: string
          amount_received: number
          currency_received?: string
          exchange_rate: number
          transfer_fee?: number
          transfer_method?: string | null
          purpose?: string | null
          status?: string
          transfer_date: string
          completion_date?: string | null
          reference_number?: string | null
        }
        Update: {
          recipient_name?: string
          recipient_country?: string
          amount_sent?: number
          currency_sent?: string
          amount_received?: number
          currency_received?: string
          exchange_rate?: number
          transfer_fee?: number
          transfer_method?: string | null
          purpose?: string | null
          status?: string
          transfer_date?: string
          completion_date?: string | null
          reference_number?: string | null
        }
      }
      scheduled_payments: {
        Row: {
          id: string
          user_id: string
          payment_name: string
          amount: number
          currency: string
          category: string | null
          recipient: string | null
          due_date: string
          frequency: string | null
          is_recurring: boolean
          auto_pay: boolean
          status: string
          reminder_days: number
          notes: string | null
          created_at: string
          updated_at: string
        }
        Insert: {
          user_id: string
          payment_name: string
          amount: number
          currency?: string
          category?: string | null
          recipient?: string | null
          due_date: string
          frequency?: string | null
          is_recurring?: boolean
          auto_pay?: boolean
          status?: string
          reminder_days?: number
          notes?: string | null
        }
        Update: {
          payment_name?: string
          amount?: number
          currency?: string
          category?: string | null
          recipient?: string | null
          due_date?: string
          frequency?: string | null
          is_recurring?: boolean
          auto_pay?: boolean
          status?: string
          reminder_days?: number
          notes?: string | null
        }
      }
      loan_payments: {
        Row: {
          id: string
          loan_id: string
          user_id: string
          amount: number
          principal_amount: number | null
          interest_amount: number | null
          payment_date: string
          payment_method: string | null
          notes: string | null
          created_at: string
        }
        Insert: {
          loan_id: string
          user_id: string
          amount: number
          principal_amount?: number | null
          interest_amount?: number | null
          payment_date: string
          payment_method?: string | null
          notes?: string | null
        }
        Update: {
          amount?: number
          principal_amount?: number | null
          interest_amount?: number | null
          payment_date?: string
          payment_method?: string | null
          notes?: string | null
        }
      }
    }
  }
}
