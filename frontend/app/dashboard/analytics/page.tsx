"use client"

export const dynamic = "force-dynamic"

import { useCallback, useEffect, useState } from "react"
import { useAuth } from "@/components/auth/auth-provider"
import { API, apiRequest } from "@/lib/api"
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

interface FinancialTrendRow {
  period: string
  metric: string
  currency: string
  totalAmount: number
  recordCount: number
}

interface FinancialReportRow {
  metric: string
  category: string
  currency: string
  totalAmount: number
  recordCount: number
  snapshotDate: string | null
}

export default function AnalyticsPage() {
  const { accessToken, isLoading: authLoading } = useAuth()
  const [data, setData] = useState<AnalyticsState | null>(null)
  const [loading, setLoading] = useState(true)
  const [selectedPeriod, setSelectedPeriod] = useState("6months")

  const fetchAnalyticsData = useCallback(async () => {
    if (authLoading) return
    if (!accessToken) {
      setData(null)
      setLoading(false)
      return
    }

    setLoading(true)
    try {
      const endDate = new Date()
      const startDate = new Date()
      const monthsBack = selectedPeriod === "3months" ? 3 : selectedPeriod === "6months" ? 6 : 12
      startDate.setMonth(endDate.getMonth() - monthsBack)
      startDate.setDate(1)

      // Use local calendar dates rather than UTC conversion so users west/east
      // of UTC do not accidentally shift the reporting window by one day.
      const asLocalDate = (date: Date) => {
        const year = date.getFullYear()
        const month = String(date.getMonth() + 1).padStart(2, "0")
        const day = String(date.getDate()).padStart(2, "0")
        return `${year}-${month}-${day}`
      }
      const query = `?from=${asLocalDate(startDate)}&to=${asLocalDate(endDate)}`
      const [monthlyRows, weeklyRows, categoryRows] = await Promise.all([
        apiRequest<FinancialTrendRow[]>(`${API.analyticsMonthlyTrends}${query}`, accessToken),
        apiRequest<FinancialTrendRow[]>(`${API.analyticsWeeklyTrends}${query}`, accessToken),
        apiRequest<FinancialReportRow[]>(`${API.analyticsCategories}${query}`, accessToken),
      ])

      const monthBuckets: Record<string, { income: number; expenses: number }> = {}
      for (let index = 0; index < monthsBack; index++) {
        const month = new Date(startDate.getFullYear(), startDate.getMonth() + index, 1)
        const key = `${month.getFullYear()}-${String(month.getMonth() + 1).padStart(2, "0")}`
        monthBuckets[key] = { income: 0, expenses: 0 }
      }

      monthlyRows.forEach((row) => {
        const bucket = monthBuckets[row.period]
        if (!bucket) return
        const amount = toUSD(Number(row.totalAmount), row.currency || "USD")
        if (row.metric === "INCOME" || row.metric === "TRANSACTION_INCOME") {
          bucket.income += amount
        } else {
          bucket.expenses += amount
        }
      })

      const monthlyTrends: MonthlyTrend[] = Object.entries(monthBuckets).map(([period, values]) => {
        const date = new Date(`${period}-01T00:00:00`)
        return {
          month: date.toLocaleDateString("en-US", { month: "short", year: "2-digit" }),
          income: Math.round(values.income * 100) / 100,
          expenses: Math.round(values.expenses * 100) / 100,
          net: Math.round((values.income - values.expenses) * 100) / 100,
        }
      })

      const weeklyMap = new Map<string, number>()
      weeklyRows.forEach((row) => {
        if (row.metric === "INCOME" || row.metric === "TRANSACTION_INCOME") return
        const weekStart = row.period
        weeklyMap.set(weekStart, (weeklyMap.get(weekStart) ?? 0) + toUSD(Number(row.totalAmount), row.currency || "USD"))
      })
      const weeklySpend: WeeklySpend[] = Array.from(weeklyMap.entries())
        .sort(([left], [right]) => left.localeCompare(right))
        .slice(-8)
        .map(([weekStart, amount]) => ({
          week: new Date(`${weekStart}T00:00:00`).toLocaleDateString("en-US", { month: "short", day: "numeric" }),
          amount: Math.round(amount * 100) / 100,
        }))

      const categoryTotals = new Map<string, number>()
      categoryRows.forEach((row) => {
        if (row.metric === "INCOME" || row.metric === "TRANSACTION_INCOME" || row.metric === "LOAN_OUTSTANDING") return
        const category = row.category || "Other"
        categoryTotals.set(category, (categoryTotals.get(category) ?? 0) + toUSD(Number(row.totalAmount), row.currency || "USD"))
      })
      const totalCategorySpend = Array.from(categoryTotals.values()).reduce((sum, value) => sum + value, 0)
      const categoryBreakdown: CategoryData[] = Array.from(categoryTotals.entries())
        .map(([category, amount]) => ({
          category,
          amount: Math.round(amount * 100) / 100,
          percentage: totalCategorySpend > 0 ? (amount / totalCategorySpend) * 100 : 0,
        }))
        .sort((left, right) => right.amount - left.amount)
        .slice(0, 6)

      const totalIncome = monthlyTrends.reduce((sum, month) => sum + month.income, 0)
      const totalExpenses = monthlyTrends.reduce((sum, month) => sum + month.expenses, 0)
      const netSavings = totalIncome - totalExpenses
      const savingsRate = totalIncome > 0 ? (netSavings / totalIncome) * 100 : 0

      setData({ totalIncome, totalExpenses, netSavings, savingsRate, monthlyTrends, weeklySpend, categoryBreakdown })
    } catch (error) {
      console.error("Error fetching analytics data:", error)
      setData(null)
    } finally {
      setLoading(false)
    }
  }, [accessToken, authLoading, selectedPeriod])

  useEffect(() => {
    void fetchAnalyticsData()
  }, [fetchAnalyticsData])

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
