import { createClient } from "@supabase/supabase-js"

const supabaseUrl = process.env.NEXT_PUBLIC_SUPABASE_URL!
const supabaseAnonKey = process.env.NEXT_PUBLIC_SUPABASE_ANON_KEY!

export const supabase = createClient(supabaseUrl, supabaseAnonKey)

// Enhanced error handling function
export const handleSupabaseError = (error: any, operation: string) => {
  console.error(`Supabase error during ${operation}:`, error)

  // Handle specific error types
  if (error?.code === "PGRST116") {
    console.error("Schema error: Missing table or column. Please run the database migration scripts.")
    throw new Error(`Database schema error: ${error.message}. Please contact support.`)
  }

  if (error?.code === "42703") {
    console.error("Column does not exist error:", error.message)
    throw new Error("Database schema is outdated. Please run the currency fix script.")
  }

  if (error?.code === "23505") {
    throw new Error("This record already exists. Please check your data.")
  }

  if (error?.code === "23503") {
    throw new Error("Cannot delete this record as it is referenced by other data.")
  }

  if (error?.code === "PGRST301") {
    throw new Error("You do not have permission to perform this action.")
  }

  // Generic error handling
  throw new Error(error?.message || `Failed to ${operation}. Please try again.`)
}

// Database types for better TypeScript support
export interface Database {
  public: {
    Tables: {
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
          currency: string
          created_at: string
          updated_at: string
        }
        Insert: {
          id?: string
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
          currency?: string
          created_at?: string
          updated_at?: string
        }
        Update: {
          id?: string
          user_id?: string
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
          currency?: string
          created_at?: string
          updated_at?: string
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
          currency: string
          created_at: string
        }
        Insert: {
          id?: string
          loan_id: string
          user_id: string
          amount: number
          principal_amount?: number | null
          interest_amount?: number | null
          payment_date: string
          payment_method?: string | null
          notes?: string | null
          currency?: string
          created_at?: string
        }
        Update: {
          id?: string
          loan_id?: string
          user_id?: string
          amount?: number
          principal_amount?: number | null
          interest_amount?: number | null
          payment_date?: string
          payment_method?: string | null
          notes?: string | null
          currency?: string
          created_at?: string
        }
      }
    }
  }
}
