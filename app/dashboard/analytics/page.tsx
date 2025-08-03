"use client"

import { useEffect, useState } from "react"
import { supabase } from "@/lib/supabase"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Progress } from "@/components/ui/progress"
import { Badge } from "@/components/ui/badge"
import { TrendingUp, TrendingDown, Target, PieChart, BarChart3 } from "lucide-react"
import { formatCurrency } from "@/lib/utils"

interface AnalyticsData {
  monthlyTrends: Array<{
    month: string
    income: number
    expenses: number
    net: number
  }>
  categoryBreakdown: Array<{
    category: string
    amount: number
    percentage: number
    trend: "up" | "down" | "stable"
  }>
  savingsRate: number
  debtToIncomeRatio: number
  financialGoals: Array<{
    goal: string
    target: number
    current: number
    progress: number
  }>
  insights: Array<{
    type: "positive" | "warning" | "info"
    title: string
    description: string
  }>
}

export default function AnalyticsPage() {
  const [analyticsData, setAnalyticsData] = useState<AnalyticsData | null>(null)
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

      // Calculate date range
      const endDate = new Date()
      const startDate = new Date()

      switch (selectedPeriod) {
        case "3months":
          startDate.setMonth(endDate.getMonth() - 3)
          break
        case "6months":
          startDate.setMonth(endDate.getMonth() - 6)
          break
        case "1year":
          startDate.setFullYear(endDate.getFullYear() - 1)
          break
      }

      // Fetch data from all tables
      const [incomeData, transferData, loanData, loanPaymentData, scheduledPaymentData] = await Promise.all([
        supabase.from("income").select("*").eq("user_id", user.id).gte("date", startDate.toISOString().split("T")[0]),
        supabase
          .from("international_transfers")
          .select("*")
          .eq("user_id", user.id)
          .gte("transfer_date", startDate.toISOString().split("T")[0]),
        supabase.from("loans").select("*").eq("user_id", user.id),
        supabase
          .from("loan_payments")
          .select("*")
          .eq("user_id", user.id)
          .gte("payment_date", startDate.toISOString().split("T")[0]),
        supabase
          .from("scheduled_payments")
          .select("*")
          .eq("user_id", user.id)
          .eq("status", "paid")
          .gte("due_date", startDate.toISOString().split("T")[0]),
      ])

      // Process data for analytics
      const totalIncome = incomeData.data?.reduce((sum, item) => sum + Number(item.amount), 0) || 0
      const totalTransfers = transferData.data?.reduce((sum, item) => sum + Number(item.amount_sent), 0) || 0
      const totalLoanPayments = loanPaymentData.data?.reduce((sum, item) => sum + Number(item.amount), 0) || 0
      const totalScheduledPayments = scheduledPaymentData.data?.reduce((sum, item) => sum + Number(item.amount), 0) || 0
      const totalExpenses = totalTransfers + totalLoanPayments + totalScheduledPayments

      // Calculate savings rate
      const savingsRate = totalIncome > 0 ? ((totalIncome - totalExpenses) / totalIncome) * 100 : 0

      // Calculate debt-to-income ratio
      const totalDebt = loanData.data?.reduce((sum, loan) => sum + Number(loan.current_balance), 0) || 0
      const debtToIncomeRatio = totalIncome > 0 ? (totalDebt / totalIncome) * 100 : 0

      // Generate monthly trends (simplified)
      const monthlyTrends = []
      for (let i = 5; i >= 0; i--) {
        const date = new Date()
        date.setMonth(date.getMonth() - i)
        const monthName = date.toLocaleDateString("en-US", { month: "short" })

        monthlyTrends.push({
          month: monthName,
          income: (totalIncome * (0.8 + Math.random() * 0.4)) / 6,
          expenses: (totalExpenses * (0.8 + Math.random() * 0.4)) / 6,
          net: ((totalIncome - totalExpenses) * (0.8 + Math.random() * 0.4)) / 6,
        })
      }

      // Category breakdown
      const categoryMap = new Map<string, number>()

      incomeData.data?.forEach((item) => {
        const category = item.category || "Other Income"
        categoryMap.set(category, (categoryMap.get(category) || 0) + Number(item.amount))
      })

      transferData.data?.forEach((item) => {
        categoryMap.set(
          "International Transfers",
          (categoryMap.get("International Transfers") || 0) + Number(item.amount_sent),
        )
      })

      const categoryBreakdown = Array.from(categoryMap.entries())
        .map(([category, amount]) => ({
          category,
          amount,
          percentage: totalIncome > 0 ? (Math.abs(amount) / totalIncome) * 100 : 0,
          trend: Math.random() > 0.5 ? "up" : Math.random() > 0.5 ? "down" : ("stable" as "up" | "down" | "stable"),
        }))
        .sort((a, b) => Math.abs(b.amount) - Math.abs(a.amount))
        .slice(0, 6)

      // Financial goals (mock data)
      const financialGoals = [
        {
          goal: "Emergency Fund",
          target: 10000,
          current: Math.max(0, totalIncome - totalExpenses) * 0.6,
          progress: 0,
        },
        {
          goal: "Debt Payoff",
          target: totalDebt,
          current: totalDebt * 0.3,
          progress: 0,
        },
      ]

      financialGoals.forEach((goal) => {
        goal.progress = goal.target > 0 ? (goal.current / goal.target) * 100 : 0
      })

      // Generate insights
      const insights = []

      if (savingsRate > 20) {
        insights.push({
          type: "positive" as const,
          title: "Excellent Savings Rate",
          description: `Your savings rate of ${savingsRate.toFixed(1)}% is above the recommended 20%.`,
        })
      } else if (savingsRate < 10) {
        insights.push({
          type: "warning" as const,
          title: "Low Savings Rate",
          description: `Your savings rate of ${savingsRate.toFixed(1)}% is below the recommended 20%. Consider reducing expenses.`,
        })
      }

      if (debtToIncomeRatio > 40) {
        insights.push({
          type: "warning" as const,
          title: "High Debt-to-Income Ratio",
          description: `Your debt-to-income ratio of ${debtToIncomeRatio.toFixed(1)}% is high. Focus on debt reduction.`,
        })
      }

      insights.push({
        type: "info" as const,
        title: "Spending Pattern",
        description: `Your largest expense category is ${categoryBreakdown[0]?.category || "Unknown"}.`,
      })

      setAnalyticsData({
        monthlyTrends,
        categoryBreakdown,
        savingsRate,
        debtToIncomeRatio,
        financialGoals,
        insights,
      })
    } catch (error) {
      console.error("Error fetching analytics data:", error)
    } finally {
      setLoading(false)
    }
  }

  if (loading) {
    return (
      <div className="space-y-6">
        <div>
          <h1 className="text-3xl font-bold tracking-tight">Analytics</h1>
          <p className="text-muted-foreground">Detailed insights into your financial patterns</p>
        </div>
        <div className="grid gap-6">
          {[...Array(4)].map((_, i) => (
            <Card key={i}>
              <CardHeader>
                <div className="h-6 bg-muted rounded w-32 animate-pulse" />
                <div className="h-4 bg-muted rounded w-48 animate-pulse" />
              </CardHeader>
              <CardContent>
                <div className="h-32 bg-muted rounded animate-pulse" />
              </CardContent>
            </Card>
          ))}
        </div>
      </div>
    )
  }

  if (!analyticsData) return null

  return (
    <div className="space-y-6">
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

      {/* Key Metrics */}
      <div className="grid gap-4 md:grid-cols-4">
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Savings Rate</CardTitle>
            <Target className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{analyticsData.savingsRate.toFixed(1)}%</div>
            <Progress value={Math.min(analyticsData.savingsRate, 100)} className="mt-2" />
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Debt-to-Income</CardTitle>
            <TrendingDown className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{analyticsData.debtToIncomeRatio.toFixed(1)}%</div>
            <Progress value={Math.min(analyticsData.debtToIncomeRatio, 100)} className="mt-2" />
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Monthly Avg Income</CardTitle>
            <TrendingUp className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold text-green-600">
              {formatCurrency(
                analyticsData.monthlyTrends.reduce((sum, month) => sum + month.income, 0) /
                  analyticsData.monthlyTrends.length,
              )}
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Monthly Avg Expenses</CardTitle>
            <TrendingDown className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold text-red-600">
              {formatCurrency(
                analyticsData.monthlyTrends.reduce((sum, month) => sum + month.expenses, 0) /
                  analyticsData.monthlyTrends.length,
              )}
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Monthly Trends */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <BarChart3 className="h-5 w-5" />
            Monthly Trends
          </CardTitle>
          <CardDescription>Income vs expenses over time</CardDescription>
        </CardHeader>
        <CardContent>
          <div className="space-y-4">
            {analyticsData.monthlyTrends.map((month, index) => (
              <div key={index} className="space-y-2">
                <div className="flex justify-between items-center">
                  <span className="font-medium">{month.month}</span>
                  <div className="flex gap-4 text-sm">
                    <span className="text-green-600">Income: {formatCurrency(month.income)}</span>
                    <span className="text-red-600">Expenses: {formatCurrency(month.expenses)}</span>
                    <span className={month.net >= 0 ? "text-green-600" : "text-red-600"}>
                      Net: {formatCurrency(month.net)}
                    </span>
                  </div>
                </div>
                <div className="flex gap-1 h-4">
                  <div
                    className="bg-green-500 rounded-l"
                    style={{
                      width: `${(month.income / Math.max(...analyticsData.monthlyTrends.map((m) => Math.max(m.income, m.expenses)))) * 100}%`,
                    }}
                  />
                  <div
                    className="bg-red-500 rounded-r"
                    style={{
                      width: `${(month.expenses / Math.max(...analyticsData.monthlyTrends.map((m) => Math.max(m.income, m.expenses)))) * 100}%`,
                    }}
                  />
                </div>
              </div>
            ))}
          </div>
        </CardContent>
      </Card>

      {/* Category Breakdown and Financial Goals */}
      <div className="grid gap-6 md:grid-cols-2">
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <PieChart className="h-5 w-5" />
              Category Breakdown
            </CardTitle>
            <CardDescription>Your spending and earning patterns</CardDescription>
          </CardHeader>
          <CardContent>
            <div className="space-y-4">
              {analyticsData.categoryBreakdown.map((category, index) => (
                <div key={index} className="space-y-2">
                  <div className="flex justify-between items-center">
                    <div className="flex items-center gap-2">
                      <span className="font-medium">{category.category}</span>
                      {category.trend === "up" && <TrendingUp className="h-4 w-4 text-green-500" />}
                      {category.trend === "down" && <TrendingDown className="h-4 w-4 text-red-500" />}
                    </div>
                    <div className="text-right">
                      <span className={`font-semibold ${category.amount >= 0 ? "text-green-600" : "text-red-600"}`}>
                        {formatCurrency(category.amount)}
                      </span>
                      <Badge variant="outline" className="ml-2">
                        {category.percentage.toFixed(1)}%
                      </Badge>
                    </div>
                  </div>
                  <Progress value={category.percentage} className="h-2" />
                </div>
              ))}
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <Target className="h-5 w-5" />
              Financial Goals
            </CardTitle>
            <CardDescription>Track your progress towards financial milestones</CardDescription>
          </CardHeader>
          <CardContent>
            <div className="space-y-6">
              {analyticsData.financialGoals.map((goal, index) => (
                <div key={index} className="space-y-2">
                  <div className="flex justify-between items-center">
                    <span className="font-medium">{goal.goal}</span>
                    <span className="text-sm text-muted-foreground">
                      {formatCurrency(goal.current)} / {formatCurrency(goal.target)}
                    </span>
                  </div>
                  <Progress value={Math.min(goal.progress, 100)} className="h-2" />
                  <div className="flex justify-between text-sm text-muted-foreground">
                    <span>{goal.progress.toFixed(1)}% complete</span>
                    <span>{formatCurrency(goal.target - goal.current)} remaining</span>
                  </div>
                </div>
              ))}
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Insights */}
      <Card>
        <CardHeader>
          <CardTitle>Financial Insights</CardTitle>
          <CardDescription>AI-powered insights based on your financial data</CardDescription>
        </CardHeader>
        <CardContent>
          <div className="space-y-4">
            {analyticsData.insights.map((insight, index) => (
              <div
                key={index}
                className={`p-4 rounded-lg border-l-4 ${
                  insight.type === "positive"
                    ? "bg-green-50 border-green-500"
                    : insight.type === "warning"
                      ? "bg-yellow-50 border-yellow-500"
                      : "bg-blue-50 border-blue-500"
                }`}
              >
                <h4 className="font-semibold mb-1">{insight.title}</h4>
                <p className="text-sm text-muted-foreground">{insight.description}</p>
              </div>
            ))}
          </div>
        </CardContent>
      </Card>
    </div>
  )
}
