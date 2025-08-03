"use client"

import { useEffect, useState } from "react"
import { supabase } from "@/lib/supabase"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"
import { Progress } from "@/components/ui/progress"
import { formatCurrency, convertCurrency } from "@/lib/utils"
import { DollarSign, CreditCard, Send, TrendingUp, TrendingDown, Calendar, AlertCircle } from "lucide-react"

interface DashboardData {
  totalIncome: number
  totalLoans: number
  totalTransfers: number
  upcomingPayments: number
  recentActivity: any[]
}

export default function DashboardOverview() {
  const [data, setData] = useState<DashboardData>({
    totalIncome: 0,
    totalLoans: 0,
    totalTransfers: 0,
    upcomingPayments: 0,
    recentActivity: [],
  })
  const [loading, setLoading] = useState(true)
  const [exchangeRate, setExchangeRate] = useState(83.5)

  useEffect(() => {
    const fetchDashboardData = async () => {
      try {
        const {
          data: { user },
        } = await supabase.auth.getUser()
        if (!user) return

        // Fetch income data
        const { data: incomeData } = await supabase.from("income").select("amount").eq("user_id", user.id)

        // Fetch loans data
        const { data: loansData } = await supabase.from("loans").select("current_balance").eq("user_id", user.id)

        // Fetch transfers data
        const { data: transfersData } = await supabase
          .from("international_transfers")
          .select("amount_sent")
          .eq("user_id", user.id)

        // Fetch upcoming payments
        const { data: paymentsData } = await supabase
          .from("scheduled_payments")
          .select("amount")
          .eq("user_id", user.id)
          .eq("status", "pending")

        setData({
          totalIncome: incomeData?.reduce((sum, item) => sum + item.amount, 0) || 0,
          totalLoans: loansData?.reduce((sum, item) => sum + item.current_balance, 0) || 0,
          totalTransfers: transfersData?.reduce((sum, item) => sum + item.amount_sent, 0) || 0,
          upcomingPayments: paymentsData?.length || 0,
          recentActivity: [],
        })
      } catch (error) {
        console.error("Error fetching dashboard data:", error)
      } finally {
        setLoading(false)
      }
    }

    fetchDashboardData()
  }, [])

  if (loading) {
    return (
      <div className="space-y-6">
        <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
          {[...Array(4)].map((_, i) => (
            <Card key={i} className="animate-pulse">
              <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                <div className="h-4 bg-gray-200 dark:bg-gray-700 rounded w-20"></div>
                <div className="h-4 w-4 bg-gray-200 dark:bg-gray-700 rounded"></div>
              </CardHeader>
              <CardContent>
                <div className="h-8 bg-gray-200 dark:bg-gray-700 rounded w-24 mb-2"></div>
                <div className="h-3 bg-gray-200 dark:bg-gray-700 rounded w-32"></div>
              </CardContent>
            </Card>
          ))}
        </div>
      </div>
    )
  }

  return (
    <div className="space-y-6">
      <div>
        <h2 className="text-3xl font-bold tracking-tight">Dashboard</h2>
        <p className="text-muted-foreground">Here's an overview of your financial status</p>
      </div>

      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
        {/* Total Income Card */}
        <Card className="border-l-4 border-l-green-500 dark:border-l-green-400">
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Total Income</CardTitle>
            <DollarSign className="h-4 w-4 text-green-600 dark:text-green-400" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold text-green-600 dark:text-green-400">
              {formatCurrency(data.totalIncome, "USD")}
            </div>
            <p className="text-xs text-muted-foreground">
              ≈ {formatCurrency(convertCurrency(data.totalIncome, exchangeRate), "INR")}
            </p>
            <div className="flex items-center pt-1">
              <TrendingUp className="h-3 w-3 text-green-600 dark:text-green-400 mr-1" />
              <span className="text-xs text-green-600 dark:text-green-400">+12% from last month</span>
            </div>
          </CardContent>
        </Card>

        {/* Total Loans Card */}
        <Card className="border-l-4 border-l-red-500 dark:border-l-red-400">
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Outstanding Loans</CardTitle>
            <CreditCard className="h-4 w-4 text-red-600 dark:text-red-400" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold text-red-600 dark:text-red-400">
              {formatCurrency(data.totalLoans, "INR")}
            </div>
            <p className="text-xs text-muted-foreground">Active loan balances</p>
            <div className="flex items-center pt-1">
              <TrendingDown className="h-3 w-3 text-red-600 dark:text-red-400 mr-1" />
              <span className="text-xs text-red-600 dark:text-red-400">-5% from last month</span>
            </div>
          </CardContent>
        </Card>

        {/* International Transfers Card */}
        <Card className="border-l-4 border-l-blue-500 dark:border-l-blue-400">
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Transfers Sent</CardTitle>
            <Send className="h-4 w-4 text-blue-600 dark:text-blue-400" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold text-blue-600 dark:text-blue-400">
              {formatCurrency(data.totalTransfers, "USD")}
            </div>
            <p className="text-xs text-muted-foreground">
              ≈ {formatCurrency(convertCurrency(data.totalTransfers, exchangeRate), "INR")}
            </p>
            <div className="flex items-center pt-1">
              <TrendingUp className="h-3 w-3 text-blue-600 dark:text-blue-400 mr-1" />
              <span className="text-xs text-blue-600 dark:text-blue-400">+8% from last month</span>
            </div>
          </CardContent>
        </Card>

        {/* Upcoming Payments Card */}
        <Card className="border-l-4 border-l-orange-500 dark:border-l-orange-400">
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Upcoming Payments</CardTitle>
            <Calendar className="h-4 w-4 text-orange-600 dark:text-orange-400" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold text-orange-600 dark:text-orange-400">{data.upcomingPayments}</div>
            <p className="text-xs text-muted-foreground">Scheduled this month</p>
            {data.upcomingPayments > 0 && (
              <div className="flex items-center pt-1">
                <AlertCircle className="h-3 w-3 text-orange-600 dark:text-orange-400 mr-1" />
                <span className="text-xs text-orange-600 dark:text-orange-400">Action required</span>
              </div>
            )}
          </CardContent>
        </Card>
      </div>

      {/* Quick Actions */}
      <Card>
        <CardHeader>
          <CardTitle>Quick Actions</CardTitle>
          <CardDescription>Common tasks to manage your finances</CardDescription>
        </CardHeader>
        <CardContent>
          <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
            <div className="flex flex-col items-center p-4 border rounded-lg hover:bg-accent cursor-pointer transition-colors">
              <DollarSign className="h-8 w-8 text-green-600 dark:text-green-400 mb-2" />
              <span className="text-sm font-medium">Add Income</span>
            </div>
            <div className="flex flex-col items-center p-4 border rounded-lg hover:bg-accent cursor-pointer transition-colors">
              <CreditCard className="h-8 w-8 text-red-600 dark:text-red-400 mb-2" />
              <span className="text-sm font-medium">Track Loan</span>
            </div>
            <div className="flex flex-col items-center p-4 border rounded-lg hover:bg-accent cursor-pointer transition-colors">
              <Send className="h-8 w-8 text-blue-600 dark:text-blue-400 mb-2" />
              <span className="text-sm font-medium">Send Transfer</span>
            </div>
            <div className="flex flex-col items-center p-4 border rounded-lg hover:bg-accent cursor-pointer transition-colors">
              <Calendar className="h-8 w-8 text-orange-600 dark:text-orange-400 mb-2" />
              <span className="text-sm font-medium">Schedule Payment</span>
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Financial Health Overview */}
      <div className="grid gap-4 md:grid-cols-2">
        <Card>
          <CardHeader>
            <CardTitle>Financial Health Score</CardTitle>
            <CardDescription>Based on your income, expenses, and savings</CardDescription>
          </CardHeader>
          <CardContent>
            <div className="space-y-4">
              <div className="flex items-center justify-between">
                <span className="text-sm font-medium">Overall Score</span>
                <Badge
                  variant="secondary"
                  className="bg-green-100 text-green-800 dark:bg-green-900 dark:text-green-200"
                >
                  Good
                </Badge>
              </div>
              <Progress value={75} className="h-2" />
              <p className="text-xs text-muted-foreground">
                75/100 - You're doing well! Consider increasing your savings rate.
              </p>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Monthly Summary</CardTitle>
            <CardDescription>Your financial activity this month</CardDescription>
          </CardHeader>
          <CardContent>
            <div className="space-y-4">
              <div className="flex items-center justify-between">
                <span className="text-sm">Income</span>
                <span className="text-sm font-medium text-green-600 dark:text-green-400">
                  +{formatCurrency(data.totalIncome * 0.1, "USD")}
                </span>
              </div>
              <div className="flex items-center justify-between">
                <span className="text-sm">Loan Payments</span>
                <span className="text-sm font-medium text-red-600 dark:text-red-400">
                  -{formatCurrency(data.totalLoans * 0.05, "INR")}
                </span>
              </div>
              <div className="flex items-center justify-between">
                <span className="text-sm">Transfers</span>
                <span className="text-sm font-medium text-blue-600 dark:text-blue-400">
                  -{formatCurrency(data.totalTransfers * 0.1, "USD")}
                </span>
              </div>
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  )
}
