"use client"

import { useEffect, useState } from "react"
import { useRouter } from "next/navigation"
import { supabase } from "@/lib/supabase"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { Badge } from "@/components/ui/badge"
import { Alert, AlertDescription } from "@/components/ui/alert"
import {
  DollarSign,
  CreditCard,
  TrendingUp,
  Users,
  ArrowUpRight,
  ArrowDownRight,
  Plus,
  FileText,
  BarChart3,
  AlertCircle,
  IndianRupee,
  Send,
  Calendar,
  History,
} from "lucide-react"
import { formatINR, convertCurrency } from "@/lib/utils"

interface DashboardData {
  totalIncome: number
  totalLoans: number
  totalTransfers: number
  activeLoans: number
  recentTransactions: any[]
  monthlyIncome: number
  monthlyExpenses: number
}

export default function DashboardOverview() {
  const router = useRouter()
  const [data, setData] = useState<DashboardData>({
    totalIncome: 0,
    totalLoans: 0,
    totalTransfers: 0,
    activeLoans: 0,
    recentTransactions: [],
    monthlyIncome: 0,
    monthlyExpenses: 0,
  })
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState("")

  useEffect(() => {
    fetchDashboardData()
  }, [])

  const fetchDashboardData = async () => {
    try {
      setError("")
      const {
        data: { user },
      } = await supabase.auth.getUser()

      if (!user) {
        setError("Please log in to view dashboard")
        return
      }

      // Fetch income data
      const { data: incomeData, error: incomeError } = await supabase
        .from("income")
        .select("amount, currency")
        .eq("user_id", user.id)

      if (incomeError) {
        console.error("Error fetching income:", incomeError)
      }

      // Fetch loans data
      const { data: loansData, error: loansError } = await supabase
        .from("loans")
        .select("current_balance, currency, status")
        .eq("user_id", user.id)

      if (loansError) {
        console.error("Error fetching loans:", loansError)
      }

      // Fetch transfers data
      const { data: transfersData, error: transfersError } = await supabase
        .from("international_transfers")
        .select("amount, currency")
        .eq("user_id", user.id)

      if (transfersError) {
        console.error("Error fetching transfers:", transfersError)
      }

      // Calculate totals (convert everything to INR for consistency)
      const totalIncome = (incomeData || []).reduce((sum, item) => {
        const amountInINR = item.currency === "USD" ? convertCurrency(item.amount, "USD", "INR") : item.amount
        return sum + amountInINR
      }, 0)

      const totalLoans = (loansData || []).reduce((sum, item) => {
        const amountInINR =
          item.currency === "USD" ? convertCurrency(item.current_balance, "USD", "INR") : item.current_balance
        return sum + amountInINR
      }, 0)

      const totalTransfers = (transfersData || []).reduce((sum, item) => {
        const amountInINR = item.currency === "USD" ? convertCurrency(item.amount, "USD", "INR") : item.amount
        return sum + amountInINR
      }, 0)

      const activeLoans = (loansData || []).filter((loan) => loan.status === "active").length

      setData({
        totalIncome,
        totalLoans,
        totalTransfers,
        activeLoans,
        recentTransactions: [],
        monthlyIncome: totalIncome,
        monthlyExpenses: totalLoans,
      })
    } catch (error: any) {
      console.error("Error fetching dashboard data:", error)
      setError(error.message || "Failed to fetch dashboard data")
    } finally {
      setLoading(false)
    }
  }

  const handleNavigation = (path: string) => {
    router.push(path)
  }

  if (loading) {
    return (
      <div className="space-y-6">
        <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
          {[...Array(4)].map((_, i) => (
            <Card key={i}>
              <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                <div className="h-4 bg-muted rounded w-24 animate-pulse" />
                <div className="h-4 w-4 bg-muted rounded animate-pulse" />
              </CardHeader>
              <CardContent>
                <div className="h-8 bg-muted rounded w-32 animate-pulse mb-2" />
                <div className="h-3 bg-muted rounded w-20 animate-pulse" />
              </CardContent>
            </Card>
          ))}
        </div>
      </div>
    )
  }

  return (
    <div className="space-y-6">
      {/* Error Message */}
      {error && (
        <Alert variant="destructive">
          <AlertCircle className="h-4 w-4" />
          <AlertDescription>{error}</AlertDescription>
        </Alert>
      )}

      {/* Welcome Section */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold tracking-tight">Dashboard</h1>
          <p className="text-muted-foreground">Welcome back! Here's an overview of your financial status.</p>
        </div>
      </div>

      {/* Overview Cards */}
      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
        <Card
          className="cursor-pointer hover:shadow-lg transition-all duration-200 hover:scale-105 border-l-4 border-l-green-500"
          onClick={() => handleNavigation("/dashboard/income")}
        >
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Total Income</CardTitle>
            <DollarSign className="h-4 w-4 text-green-600" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold text-green-600">{formatINR(data.totalIncome)}</div>
            <p className="text-xs text-muted-foreground">
              <ArrowUpRight className="inline h-3 w-3 mr-1" />
              Click to manage income
            </p>
          </CardContent>
        </Card>

        <Card
          className="cursor-pointer hover:shadow-lg transition-all duration-200 hover:scale-105 border-l-4 border-l-red-500"
          onClick={() => handleNavigation("/dashboard/loans")}
        >
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Total Debt</CardTitle>
            <CreditCard className="h-4 w-4 text-red-600" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold text-red-600">{formatINR(data.totalLoans)}</div>
            <p className="text-xs text-muted-foreground">
              <ArrowDownRight className="inline h-3 w-3 mr-1" />
              {data.activeLoans} active loans
            </p>
          </CardContent>
        </Card>

        <Card
          className="cursor-pointer hover:shadow-lg transition-all duration-200 hover:scale-105 border-l-4 border-l-blue-500"
          onClick={() => handleNavigation("/dashboard/transfers")}
        >
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Transfers</CardTitle>
            <Send className="h-4 w-4 text-blue-600" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold text-blue-600">{formatINR(data.totalTransfers)}</div>
            <p className="text-xs text-muted-foreground">
              <TrendingUp className="inline h-3 w-3 mr-1" />
              International transfers
            </p>
          </CardContent>
        </Card>

        <Card
          className="cursor-pointer hover:shadow-lg transition-all duration-200 hover:scale-105 border-l-4 border-l-purple-500"
          onClick={() => handleNavigation("/dashboard/payments")}
        >
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Net Worth</CardTitle>
            <TrendingUp className="h-4 w-4 text-purple-600" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold text-purple-600">{formatINR(data.totalIncome - data.totalLoans)}</div>
            <p className="text-xs text-muted-foreground">
              <Users className="inline h-3 w-3 mr-1" />
              Income minus debt
            </p>
          </CardContent>
        </Card>
      </div>

      {/* Quick Actions */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <Plus className="h-5 w-5" />
            Quick Actions
          </CardTitle>
          <CardDescription>Quickly access common financial management tasks</CardDescription>
        </CardHeader>
        <CardContent>
          <div className="grid gap-3 md:grid-cols-2 lg:grid-cols-4">
            <Button
              variant="outline"
              className="h-auto p-4 flex flex-col items-center gap-2 hover:bg-green-50 hover:border-green-200 dark:hover:bg-green-950/20 bg-transparent"
              onClick={() => handleNavigation("/dashboard/income")}
            >
              <DollarSign className="h-6 w-6 text-green-600" />
              <div className="text-center">
                <div className="font-medium">Add Income</div>
                <div className="text-xs text-muted-foreground">Record new income</div>
              </div>
            </Button>

            <Button
              variant="outline"
              className="h-auto p-4 flex flex-col items-center gap-2 hover:bg-red-50 hover:border-red-200 dark:hover:bg-red-950/20 bg-transparent"
              onClick={() => handleNavigation("/dashboard/loans")}
            >
              <CreditCard className="h-6 w-6 text-red-600" />
              <div className="text-center">
                <div className="font-medium">Manage Loans</div>
                <div className="text-xs text-muted-foreground">Track your debt</div>
              </div>
            </Button>

            <Button
              variant="outline"
              className="h-auto p-4 flex flex-col items-center gap-2 hover:bg-blue-50 hover:border-blue-200 dark:hover:bg-blue-950/20 bg-transparent"
              onClick={() => handleNavigation("/dashboard/transfers")}
            >
              <Send className="h-6 w-6 text-blue-600" />
              <div className="text-center">
                <div className="font-medium">Send Money</div>
                <div className="text-xs text-muted-foreground">International transfers</div>
              </div>
            </Button>

            <Button
              variant="outline"
              className="h-auto p-4 flex flex-col items-center gap-2 hover:bg-purple-50 hover:border-purple-200 dark:hover:bg-purple-950/20 bg-transparent"
              onClick={() => handleNavigation("/dashboard/payments")}
            >
              <IndianRupee className="h-6 w-6 text-purple-600" />
              <div className="text-center">
                <div className="font-medium">Record Payment</div>
                <div className="text-xs text-muted-foreground">Log loan payments</div>
              </div>
            </Button>

            <Button
              variant="outline"
              className="h-auto p-4 flex flex-col items-center gap-2 hover:bg-orange-50 hover:border-orange-200 dark:hover:bg-orange-950/20 bg-transparent"
              onClick={() => handleNavigation("/dashboard/history")}
            >
              <History className="h-6 w-6 text-orange-600" />
              <div className="text-center">
                <div className="font-medium">View History</div>
                <div className="text-xs text-muted-foreground">Transaction history</div>
              </div>
            </Button>

            <Button
              variant="outline"
              className="h-auto p-4 flex flex-col items-center gap-2 hover:bg-teal-50 hover:border-teal-200 dark:hover:bg-teal-950/20 bg-transparent"
              onClick={() => handleNavigation("/dashboard/analytics")}
            >
              <BarChart3 className="h-6 w-6 text-teal-600" />
              <div className="text-center">
                <div className="font-medium">Analytics</div>
                <div className="text-xs text-muted-foreground">Financial insights</div>
              </div>
            </Button>

            <Button
              variant="outline"
              className="h-auto p-4 flex flex-col items-center gap-2 hover:bg-gray-50 hover:border-gray-200 dark:hover:bg-gray-950/20 bg-transparent"
              onClick={() => handleNavigation("/dashboard/reports")}
            >
              <FileText className="h-6 w-6 text-gray-600" />
              <div className="text-center">
                <div className="font-medium">Reports</div>
                <div className="text-xs text-muted-foreground">Generate reports</div>
              </div>
            </Button>

            <Button
              variant="outline"
              className="h-auto p-4 flex flex-col items-center gap-2 hover:bg-indigo-50 hover:border-indigo-200 dark:hover:bg-indigo-950/20 bg-transparent"
              onClick={() => handleNavigation("/dashboard/settings")}
            >
              <Calendar className="h-6 w-6 text-indigo-600" />
              <div className="text-center">
                <div className="font-medium">Settings</div>
                <div className="text-xs text-muted-foreground">Account settings</div>
              </div>
            </Button>
          </div>
        </CardContent>
      </Card>

      {/* Recent Activity */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <History className="h-5 w-5" />
            Recent Activity
          </CardTitle>
          <CardDescription>Your latest financial transactions and updates</CardDescription>
        </CardHeader>
        <CardContent>
          <div className="space-y-4">
            <div className="flex items-center justify-between p-3 rounded-lg bg-muted/50">
              <div className="flex items-center gap-3">
                <div className="h-8 w-8 rounded-full bg-green-100 dark:bg-green-900/20 flex items-center justify-center">
                  <DollarSign className="h-4 w-4 text-green-600" />
                </div>
                <div>
                  <p className="font-medium">Income Management</p>
                  <p className="text-sm text-muted-foreground">Track your earnings in USD</p>
                </div>
              </div>
              <Badge variant="outline">Active</Badge>
            </div>

            <div className="flex items-center justify-between p-3 rounded-lg bg-muted/50">
              <div className="flex items-center gap-3">
                <div className="h-8 w-8 rounded-full bg-red-100 dark:bg-red-900/20 flex items-center justify-center">
                  <CreditCard className="h-4 w-4 text-red-600" />
                </div>
                <div>
                  <p className="font-medium">Loan Tracking</p>
                  <p className="text-sm text-muted-foreground">Monitor debt in INR</p>
                </div>
              </div>
              <Badge variant="outline">{data.activeLoans} loans</Badge>
            </div>

            <div className="flex items-center justify-between p-3 rounded-lg bg-muted/50">
              <div className="flex items-center gap-3">
                <div className="h-8 w-8 rounded-full bg-blue-100 dark:bg-blue-900/20 flex items-center justify-center">
                  <Send className="h-4 w-4 text-blue-600" />
                </div>
                <div>
                  <p className="font-medium">International Transfers</p>
                  <p className="text-sm text-muted-foreground">Send money globally</p>
                </div>
              </div>
              <Badge variant="outline">Available</Badge>
            </div>
          </div>
        </CardContent>
      </Card>
    </div>
  )
}
