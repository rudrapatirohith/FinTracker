"use client"

import { useEffect, useState } from "react"
import { supabase } from "@/lib/supabase"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Progress } from "@/components/ui/progress"
import { Badge } from "@/components/ui/badge"
import {
  LineChart,
  Line,
  BarChart,
  Bar,
  PieChart,
  Pie,
  Cell,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer,
} from "recharts"
import {
  TrendingUp,
  TrendingDown,
  DollarSign,
  PiggyBank,
  Target,
  ArrowUpRight,
} from "lucide-react"
import { toUSD, formatUSDDisplay } from "@/lib/utils"

const COLORS = ["#3b82f6", "#10b981", "#f59e0b", "#ef4444", "#8b5cf6", "#06b6d4"]

interface MonthlyTrend {
  month: string
  income: number
  expenses: number
  net: number
}

interface WeeklySpend {
  week: string
  amount: number
}

interface CategoryData {
  category: string
  amount: number
  percentage: number
}

interface AnalyticsState {
  totalIncome: number
  totalExpenses: number
  netSavings: number
  savingsRate: number
  monthlyTrends: MonthlyTrend[]
  weeklySpend: WeeklySpend[]
  categoryBreakdown: CategoryData[]
}

export default function AnalyticsPage() {
  const [data, setData] = useState<AnalyticsState | null>(null)
  const [loading, setLoading] = useState(true)
  const [selectedPeriod, setSelectedPeriod] = useState("6months")

  useEffect(() => {
    fetchAnalyticsData()
  }, [selectedPeriod])

  const fetchAnalyticsData = async () => {
    setLoading(true)
    try {
      const {
        data: { user },
      } = await supabase.auth.getUser()
      if (!user) return

      const endDate = new Date()
      const startDate = new Date()
      const monthsBack = selectedPeriod === "3months" ? 3 : selectedPeriod === "6months" ? 6 : 12
      startDate.setMonth(endDate.getMonth() - monthsBack)

      const [incomeRes, transferRes, loanPayRes, scheduledRes, expensesRes] = await Promise.all([
        supabase
          .from("income")
          .select("amount, currency, inr_equivalent, date, category")
          .eq("user_id", user.id)
          .gte("date", startDate.toISOString().split("T")[0]),
        supabase
          .from("international_transfers")
          .select("amount_sent, currency_sent, inr_equivalent, transfer_date")
          .eq("user_id", user.id)
          .gte("transfer_date", startDate.toISOString().split("T")[0]),
        supabase
          .from("loan_payments")
          .select("amount, payment_date")
          .eq("user_id", user.id)
          .gte("payment_date", startDate.toISOString().split("T")[0]),
        supabase
          .from("scheduled_payments")
          .select("amount, currency, category, due_date")
          .eq("user_id", user.id)
          .eq("status", "paid")
          .gte("due_date", startDate.toISOString().split("T")[0]),
        supabase
          .from("monthly_expenses")
          .select("inr_equivalent, category, expense_date")
          .eq("user_id", user.id)
          .gte("expense_date", startDate.toISOString().split("T")[0]),
      ])

      // Helper to get USD value from a record with inr_equivalent or direct amount
      const incomeItems = incomeRes.data || []
      const transferItems = transferRes.data || []
      const loanPayItems = loanPayRes.data || []
      const scheduledItems = scheduledRes.data || []
      const expenseItems = expensesRes.data || []

      // Build per-month buckets
      const monthBuckets: Record<string, { income: number; expenses: number }> = {}
      for (let i = monthsBack - 1; i >= 0; i--) {
        const d = new Date()
        d.setMonth(d.getMonth() - i)
        const key = d.toLocaleDateString("en-US", { month: "short", year: "2-digit" })
        monthBuckets[key] = { income: 0, expenses: 0 }
      }

      const monthKey = (dateStr: string) => {
        const d = new Date(dateStr)
        return d.toLocaleDateString("en-US", { month: "short", year: "2-digit" })
      }

      incomeItems.forEach((item) => {
        const key = monthKey(item.date)
        if (monthBuckets[key]) {
          const usd = item.inr_equivalent
            ? toUSD(item.inr_equivalent, "INR")
            : toUSD(Number(item.amount), item.currency || "USD")
          monthBuckets[key].income += usd
        }
      })

      transferItems.forEach((item) => {
        const key = monthKey(item.transfer_date)
        if (monthBuckets[key]) {
          const usd = item.inr_equivalent
            ? toUSD(item.inr_equivalent, "INR")
            : toUSD(Number(item.amount_sent), item.currency_sent || "USD")
          monthBuckets[key].expenses += usd
        }
      })

      loanPayItems.forEach((item) => {
        const key = monthKey(item.payment_date)
        if (monthBuckets[key]) {
          monthBuckets[key].expenses += toUSD(Number(item.amount), "USD")
        }
      })

      scheduledItems.forEach((item) => {
        const key = monthKey(item.due_date)
        if (monthBuckets[key]) {
          monthBuckets[key].expenses += toUSD(Number(item.amount), item.currency || "USD")
        }
      })

      expenseItems.forEach((item) => {
        const key = monthKey(item.expense_date)
        if (monthBuckets[key]) {
          monthBuckets[key].expenses += toUSD(item.inr_equivalent || 0, "INR")
        }
      })

      const monthlyTrends: MonthlyTrend[] = Object.entries(monthBuckets).map(([month, vals]) => ({
        month,
        income: Math.round(vals.income * 100) / 100,
        expenses: Math.round(vals.expenses * 100) / 100,
        net: Math.round((vals.income - vals.expenses) * 100) / 100,
      }))

      // Weekly spending (last 8 weeks)
      const weeklyMap: Record<string, number> = {}
      const now = Date.now()
      for (let w = 7; w >= 0; w--) {
        const d = new Date(now - w * 7 * 24 * 60 * 60 * 1000)
        const wk = `W${Math.ceil(d.getDate() / 7)} ${d.toLocaleDateString("en-US", { month: "short" })}`
        weeklyMap[wk] = 0
      }

      const addToWeek = (dateStr: string, usdAmt: number) => {
        const d = new Date(dateStr)
        const wk = `W${Math.ceil(d.getDate() / 7)} ${d.toLocaleDateString("en-US", { month: "short" })}`
        if (weeklyMap[wk] !== undefined) weeklyMap[wk] += usdAmt
      }

      loanPayItems.forEach((item) => addToWeek(item.payment_date, toUSD(Number(item.amount), "USD")))
      scheduledItems.forEach((item) =>
        addToWeek(item.due_date, toUSD(Number(item.amount), item.currency || "USD")),
      )
      transferItems.forEach((item) => {
        const usd = item.inr_equivalent
          ? toUSD(item.inr_equivalent, "INR")
          : toUSD(Number(item.amount_sent), item.currency_sent || "USD")
        addToWeek(item.transfer_date, usd)
      })
      expenseItems.forEach((item) =>
        addToWeek(item.expense_date, toUSD(item.inr_equivalent || 0, "INR")),
      )

      const weeklySpend: WeeklySpend[] = Object.entries(weeklyMap).map(([week, amount]) => ({
        week,
        amount: Math.round(amount * 100) / 100,
      }))

      // Category breakdown (expenses)
      const catMap: Record<string, number> = {}

      scheduledItems.forEach((item) => {
        const cat = item.category || "Other"
        catMap[cat] = (catMap[cat] || 0) + toUSD(Number(item.amount), item.currency || "USD")
      })
      transferItems.forEach((item) => {
        const usd = item.inr_equivalent
          ? toUSD(item.inr_equivalent, "INR")
          : toUSD(Number(item.amount_sent), item.currency_sent || "USD")
        catMap["Transfers"] = (catMap["Transfers"] || 0) + usd
      })
      loanPayItems.forEach((item) => {
        catMap["Loan Payments"] = (catMap["Loan Payments"] || 0) + toUSD(Number(item.amount), "USD")
      })
      expenseItems.forEach((item) => {
        const cat = item.category || "Expenses"
        catMap[cat] = (catMap[cat] || 0) + toUSD(item.inr_equivalent || 0, "INR")
      })

      const totalExp = Object.values(catMap).reduce((s, v) => s + v, 0)
      const categoryBreakdown: CategoryData[] = Object.entries(catMap)
        .map(([category, amount]) => ({
          category,
          amount: Math.round(amount * 100) / 100,
          percentage: totalExp > 0 ? (amount / totalExp) * 100 : 0,
        }))
        .sort((a, b) => b.amount - a.amount)
        .slice(0, 6)

      // Totals
      const totalIncome = monthlyTrends.reduce((s, m) => s + m.income, 0)
      const totalExpenses = monthlyTrends.reduce((s, m) => s + m.expenses, 0)
      const netSavings = totalIncome - totalExpenses
      const savingsRate = totalIncome > 0 ? (netSavings / totalIncome) * 100 : 0

      setData({
        totalIncome,
        totalExpenses,
        netSavings,
        savingsRate,
        monthlyTrends,
        weeklySpend,
        categoryBreakdown,
      })
    } catch (err) {
      console.error("Error fetching analytics data:", err)
    } finally {
      setLoading(false)
    }
  }

  const formatDollar = (value: number) => `$${value.toLocaleString("en-US", { maximumFractionDigits: 0 })}`

  if (loading) {
    return (
      <div className="space-y-6">
        <div>
          <h1 className="text-3xl font-bold tracking-tight">Analytics</h1>
          <p className="text-muted-foreground">Detailed insights into your financial patterns</p>
        </div>
        <div className="grid gap-4 md:grid-cols-4">
          {[...Array(4)].map((_, i) => (
            <Card key={i}>
              <CardHeader>
                <div className="h-4 bg-muted rounded w-24 animate-pulse" />
              </CardHeader>
              <CardContent>
                <div className="h-8 bg-muted rounded w-32 animate-pulse" />
              </CardContent>
            </Card>
          ))}
        </div>
        <Card>
          <CardContent className="h-64 bg-muted rounded animate-pulse mt-6" />
        </Card>
      </div>
    )
  }

  if (!data) return null

  const donutData = data.categoryBreakdown.map((c) => ({ name: c.category, value: c.amount }))

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold tracking-tight">Analytics</h1>
          <p className="text-muted-foreground">Detailed insights into your financial patterns</p>
        </div>
        <Select value={selectedPeriod} onValueChange={setSelectedPeriod}>
          <SelectTrigger className="w-40">
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="3months">Last 3 Months</SelectItem>
            <SelectItem value="6months">Last 6 Months</SelectItem>
            <SelectItem value="1year">Last Year</SelectItem>
          </SelectContent>
        </Select>
      </div>

      {/* Stat Cards */}
      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Total Income</CardTitle>
            <DollarSign className="h-4 w-4 text-green-600" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold text-green-600">{formatUSDDisplay(data.totalIncome)}</div>
            <p className="text-xs text-muted-foreground flex items-center gap-1 mt-1">
              <ArrowUpRight className="h-3 w-3" /> Period total
            </p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Total Expenses</CardTitle>
            <TrendingDown className="h-4 w-4 text-red-600" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold text-red-600">{formatUSDDisplay(data.totalExpenses)}</div>
            <p className="text-xs text-muted-foreground mt-1">Period total</p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Net Savings</CardTitle>
            <PiggyBank className="h-4 w-4 text-blue-600" />
          </CardHeader>
          <CardContent>
            <div className={`text-2xl font-bold ${data.netSavings >= 0 ? "text-blue-600" : "text-red-600"}`}>
              {formatUSDDisplay(data.netSavings)}
            </div>
            <p className="text-xs text-muted-foreground mt-1">Income minus expenses</p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Savings Rate</CardTitle>
            <Target className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className={`text-2xl font-bold ${data.savingsRate >= 20 ? "text-green-600" : data.savingsRate >= 10 ? "text-yellow-600" : "text-red-600"}`}>
              {data.savingsRate.toFixed(1)}%
            </div>
            <Progress value={Math.min(Math.max(data.savingsRate, 0), 100)} className="mt-2 h-1.5" />
          </CardContent>
        </Card>
      </div>

      {/* Line Chart — Monthly Income vs Expenses */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <TrendingUp className="h-5 w-5" />
            Monthly Income vs Expenses
          </CardTitle>
          <CardDescription>Income and expense trends over the selected period (USD)</CardDescription>
        </CardHeader>
        <CardContent>
          <ResponsiveContainer width="100%" height={300}>
            <LineChart data={data.monthlyTrends} margin={{ top: 5, right: 20, left: 10, bottom: 5 }}>
              <CartesianGrid strokeDasharray="3 3" className="stroke-muted" />
              <XAxis dataKey="month" tick={{ fontSize: 12 }} className="text-muted-foreground" />
              <YAxis tickFormatter={formatDollar} tick={{ fontSize: 12 }} className="text-muted-foreground" />
              <Tooltip
                formatter={(value: number) => formatUSDDisplay(value)}
                contentStyle={{
                  backgroundColor: "hsl(var(--card))",
                  border: "1px solid hsl(var(--border))",
                  borderRadius: "8px",
                  color: "hsl(var(--foreground))",
                }}
              />
              <Legend />
              <Line
                type="monotone"
                dataKey="income"
                stroke="#10b981"
                strokeWidth={2}
                dot={{ r: 4 }}
                name="Income"
              />
              <Line
                type="monotone"
                dataKey="expenses"
                stroke="#ef4444"
                strokeWidth={2}
                dot={{ r: 4 }}
                name="Expenses"
              />
              <Line
                type="monotone"
                dataKey="net"
                stroke="#3b82f6"
                strokeWidth={2}
                strokeDasharray="5 5"
                dot={{ r: 3 }}
                name="Net"
              />
            </LineChart>
          </ResponsiveContainer>
        </CardContent>
      </Card>

      {/* Donut + Bar Charts row */}
      <div className="grid gap-6 md:grid-cols-2">
        {/* Donut — Spending by Category */}
        <Card>
          <CardHeader>
            <CardTitle>Spending by Category</CardTitle>
            <CardDescription>Breakdown of expenses across categories (USD)</CardDescription>
          </CardHeader>
          <CardContent>
            {donutData.length > 0 ? (
              <ResponsiveContainer width="100%" height={260}>
                <PieChart>
                  <Pie
                    data={donutData}
                    cx="50%"
                    cy="50%"
                    innerRadius={70}
                    outerRadius={110}
                    paddingAngle={3}
                    dataKey="value"
                  >
                    {donutData.map((_, index) => (
                      <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                    ))}
                  </Pie>
                  <Tooltip
                    formatter={(value: number) => formatUSDDisplay(value)}
                    contentStyle={{
                      backgroundColor: "hsl(var(--card))",
                      border: "1px solid hsl(var(--border))",
                      borderRadius: "8px",
                      color: "hsl(var(--foreground))",
                    }}
                  />
                  <Legend />
                </PieChart>
              </ResponsiveContainer>
            ) : (
              <div className="flex items-center justify-center h-64 text-muted-foreground text-sm">
                No expense data for this period
              </div>
            )}
          </CardContent>
        </Card>

        {/* Bar Chart — Weekly Spending */}
        <Card>
          <CardHeader>
            <CardTitle>Weekly Spending Pattern</CardTitle>
            <CardDescription>Spending distribution over the last 8 weeks (USD)</CardDescription>
          </CardHeader>
          <CardContent>
            <ResponsiveContainer width="100%" height={260}>
              <BarChart data={data.weeklySpend} margin={{ top: 5, right: 10, left: 10, bottom: 5 }}>
                <CartesianGrid strokeDasharray="3 3" className="stroke-muted" />
                <XAxis dataKey="week" tick={{ fontSize: 11 }} className="text-muted-foreground" />
                <YAxis tickFormatter={formatDollar} tick={{ fontSize: 11 }} className="text-muted-foreground" />
                <Tooltip
                  formatter={(value: number) => formatUSDDisplay(value)}
                  contentStyle={{
                    backgroundColor: "hsl(var(--card))",
                    border: "1px solid hsl(var(--border))",
                    borderRadius: "8px",
                    color: "hsl(var(--foreground))",
                  }}
                />
                <Bar dataKey="amount" name="Spending" fill="#3b82f6" radius={[4, 4, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </CardContent>
        </Card>
      </div>

      {/* Top Spending Categories */}
      <Card>
        <CardHeader>
          <CardTitle>Top Spending Categories</CardTitle>
          <CardDescription>Ranked by total spend with progress indicators</CardDescription>
        </CardHeader>
        <CardContent>
          {data.categoryBreakdown.length === 0 ? (
            <p className="text-muted-foreground text-sm text-center py-8">No spending data available</p>
          ) : (
            <div className="space-y-5">
              {data.categoryBreakdown.map((cat, index) => (
                <div key={cat.category} className="space-y-1.5">
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-2">
                      <span
                        className="inline-block w-3 h-3 rounded-full"
                        style={{ backgroundColor: COLORS[index % COLORS.length] }}
                      />
                      <span className="font-medium text-sm">{cat.category}</span>
                    </div>
                    <div className="flex items-center gap-2">
                      <span className="font-semibold text-sm">{formatUSDDisplay(cat.amount)}</span>
                      <Badge variant="outline" className="text-xs">
                        {cat.percentage.toFixed(1)}%
                      </Badge>
                    </div>
                  </div>
                  <Progress
                    value={cat.percentage}
                    className="h-2"
                    style={{ "--progress-color": COLORS[index % COLORS.length] } as React.CSSProperties}
                  />
                </div>
              ))}
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  )
}
