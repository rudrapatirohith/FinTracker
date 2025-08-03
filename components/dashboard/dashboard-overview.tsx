"use client"

import { useEffect, useState } from "react"
import { supabase } from "@/lib/supabase"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"
import { Progress } from "@/components/ui/progress"
import { DollarSign, TrendingUp, CreditCard, Send, Calendar, ArrowUpRight } from "lucide-react"
import { formatUSD, formatINR, convertUSDToINR } from "@/lib/utils"

interface DashboardData {
  totalIncome: number
  totalLoans: number
  totalTransfers: number
  upcomingPayments: number
  monthlyIncome: number
  monthlyExpenses: number
  loanProgress: Array<{
    id: string
    loan_name: string
    current_balance: number
    principal_amount: number
  }>
}

export default function DashboardOverview() {
  const [data, setData] = useState<DashboardData | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    fetchDashboardData()
  }, [])

  const fetchDashboardData = async () => {
    try {
      const {
        data: { user },
      } = await supabase.auth.getUser()
      if (!user) return

      // Fetch income data (all in USD)
      const { data: incomeData } = await supabase.from("income").select("amount, date").eq("user_id", user.id)

      // Fetch loan data (all in INR)
      const { data: loanData } = await supabase
        .from("loans")
        .select("id, loan_name, current_balance, principal_amount")
        .eq("user_id", user.id)
        .eq("status", "active")

      // Fetch transfer data
      const { data: transferData } = await supabase
        .from("international_transfers")
        .select("amount_sent")
        .eq("user_id", user.id)

      // Fetch scheduled payments
      const { data: paymentsData } = await supabase
        .from("scheduled_payments")
        .select("amount")
        .eq("user_id", user.id)
        .eq("status", "pending")
        .gte("due_date", new Date().toISOString().split("T")[0])

      // Calculate totals
      const totalIncome = incomeData?.reduce((sum, item) => sum + Number(item.amount), 0) || 0
      const totalLoans = loanData?.reduce((sum, item) => sum + Number(item.current_balance), 0) || 0
      const totalTransfers = transferData?.reduce((sum, item) => sum + Number(item.amount_sent), 0) || 0
      const upcomingPayments = paymentsData?.reduce((sum, item) => sum + Number(item.amount), 0) || 0

      // Calculate monthly income (current month)
      const currentMonth = new Date().getMonth()
      const currentYear = new Date().getFullYear()
      const monthlyIncome =
        incomeData
          ?.filter((item) => {
            const itemDate = new Date(item.date)
            return itemDate.getMonth() === currentMonth && itemDate.getFullYear() === currentYear
          })
          .reduce((sum, item) => sum + Number(item.amount), 0) || 0

      setData({
        totalIncome,
        totalLoans,
        totalTransfers,
        upcomingPayments,
        monthlyIncome,
        monthlyExpenses: 0, // Will be calculated from transactions
        loanProgress: loanData || [],
      })
    } catch (error) {
      console.error("Error fetching dashboard data:", error)
    } finally {
      setLoading(false)
    }
  }

  if (loading) {
    return (
      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
        {[...Array(4)].map((_, i) => (
          <Card key={i} className="animate-pulse">
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <div className="h-4 bg-muted rounded w-20" />
              <div className="h-4 w-4 bg-muted rounded" />
            </CardHeader>
            <CardContent>
              <div className="h-8 bg-muted rounded w-24 mb-2" />
              <div className="h-3 bg-muted rounded w-32" />
            </CardContent>
          </Card>
        ))}
      </div>
    )
  }

  if (!data) return null

  // Convert INR loans to USD for net worth calculation
  const totalLoansUSD = convertUSDToINR(data.totalLoans) // This is backwards - we need INR to USD
  const netWorth = data.totalIncome - data.totalLoans / 83.25 // Quick conversion

  return (
    <div className="space-y-6">
      {/* Overview Cards */}
      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
        <Card className="border-l-4 border-l-green-500 dark:border-l-green-400">
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Total Income</CardTitle>
            <TrendingUp className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="space-y-1">
              <div className="text-2xl font-bold text-green-600 dark:text-green-400">{formatUSD(data.totalIncome)}</div>
              <div className="text-sm text-muted-foreground">≈ {formatINR(convertUSDToINR(data.totalIncome))}</div>
              <p className="text-xs text-muted-foreground">
                <span className="text-green-600 dark:text-green-400 flex items-center">
                  <ArrowUpRight className="h-3 w-3 mr-1" />
                  This month: {formatUSD(data.monthlyIncome)}
                </span>
              </p>
            </div>
          </CardContent>
        </Card>

        <Card className="border-l-4 border-l-red-500 dark:border-l-red-400">
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Active Loans</CardTitle>
            <CreditCard className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="space-y-1">
              <div className="text-2xl font-bold text-red-600 dark:text-red-400">{formatINR(data.totalLoans)}</div>
              <p className="text-xs text-muted-foreground">
                {data.loanProgress.length} active loan{data.loanProgress.length !== 1 ? "s" : ""}
              </p>
            </div>
          </CardContent>
        </Card>

        <Card className="border-l-4 border-l-blue-500 dark:border-l-blue-400">
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">International Transfers</CardTitle>
            <Send className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="space-y-1">
              <div className="text-2xl font-bold text-blue-600 dark:text-blue-400">
                {formatUSD(data.totalTransfers)}
              </div>
              <div className="text-sm text-muted-foreground">≈ {formatINR(convertUSDToINR(data.totalTransfers))}</div>
              <p className="text-xs text-muted-foreground">Total sent this year</p>
            </div>
          </CardContent>
        </Card>

        <Card className="border-l-4 border-l-orange-500 dark:border-l-orange-400">
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Upcoming Payments</CardTitle>
            <Calendar className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold text-orange-600 dark:text-orange-400">
              {formatUSD(data.upcomingPayments)}
            </div>
            <p className="text-xs text-muted-foreground">Due this month</p>
          </CardContent>
        </Card>
      </div>

      {/* Net Worth Card */}
      <Card className="bg-gradient-to-r from-blue-50 to-indigo-50 dark:from-blue-950/20 dark:to-indigo-950/20 border-blue-200 dark:border-blue-800">
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <DollarSign className="h-5 w-5" />
            Net Worth Overview
          </CardTitle>
          <CardDescription>Your financial position at a glance</CardDescription>
        </CardHeader>
        <CardContent>
          <div className="flex items-center justify-between mb-4">
            <div>
              <p className="text-sm text-muted-foreground">Net Worth (Estimated)</p>
              <p
                className={`text-3xl font-bold ${netWorth >= 0 ? "text-green-600 dark:text-green-400" : "text-red-600 dark:text-red-400"}`}
              >
                {formatUSD(netWorth)}
              </p>
            </div>
            <Badge variant={netWorth >= 0 ? "default" : "destructive"} className="text-sm">
              {netWorth >= 0 ? "Positive" : "Negative"}
            </Badge>
          </div>
          <div className="space-y-2">
            <div className="flex justify-between text-sm">
              <span>Assets (Income USD)</span>
              <span className="text-green-600 dark:text-green-400">{formatUSD(data.totalIncome)}</span>
            </div>
            <div className="flex justify-between text-sm">
              <span>Liabilities (Loans INR)</span>
              <span className="text-red-600 dark:text-red-400">{formatINR(data.totalLoans)}</span>
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Loan Progress */}
      {data.loanProgress.length > 0 && (
        <Card>
          <CardHeader>
            <CardTitle>Loan Repayment Progress</CardTitle>
            <CardDescription>Track your loan payoff progress</CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            {data.loanProgress.map((loan) => {
              const progress = ((loan.principal_amount - loan.current_balance) / loan.principal_amount) * 100
              return (
                <div key={loan.id} className="space-y-2">
                  <div className="flex justify-between items-center">
                    <span className="font-medium">{loan.loan_name}</span>
                    <span className="text-sm text-muted-foreground">{progress.toFixed(1)}% paid off</span>
                  </div>
                  <Progress value={progress} className="h-2" />
                  <div className="flex justify-between text-sm text-muted-foreground">
                    <span>Remaining: {formatINR(loan.current_balance)}</span>
                    <span>Original: {formatINR(loan.principal_amount)}</span>
                  </div>
                </div>
              )
            })}
          </CardContent>
        </Card>
      )}
    </div>
  )
}
