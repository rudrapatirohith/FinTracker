"use client"

export const dynamic = "force-dynamic"

import { useCallback, useEffect, useState } from "react"
import { useAuth } from "@/components/auth/auth-provider"
import { API, apiGetPage, apiRequest } from "@/lib/api"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Calendar } from "@/components/ui/calendar"
import { Popover, PopoverContent, PopoverTrigger } from "@/components/ui/popover"
import { Badge } from "@/components/ui/badge"
import { Progress } from "@/components/ui/progress"
import { Download, FileText, CalendarIcon, TrendingUp, TrendingDown, PieChart, BarChart3 } from "lucide-react"
import { cn, formatCurrency, toUSD, formatUSDDisplay } from "@/lib/utils"
import { format } from "date-fns"

interface ReportData {
  totalIncome: number
  totalExpenses: number
  netIncome: number
  topCategories: Array<{ category: string; amount: number; percentage: number }>
  monthlyTrends: Array<{ month: string; income: number; expenses: number }>
  loanSummary: {
    totalDebt: number
    totalPaid: number
    averageProgress: number
  }
  transferSummary: {
    totalSent: number
    totalFees: number
    averageRate: number
  }
}

const reportTypes = [
  { value: "monthly", label: "Monthly Report" },
  { value: "quarterly", label: "Quarterly Report" },
  { value: "yearly", label: "Yearly Report" },
  { value: "custom", label: "Custom Date Range" },
]

const exportFormats = [
  { value: "pdf", label: "PDF Report", icon: FileText },
  { value: "excel", label: "Excel Spreadsheet", icon: BarChart3 },
  { value: "csv", label: "CSV Data", icon: TrendingUp },
]

interface FinancialReportRow {
  metric: string
  category: string
  currency: string
  totalAmount: number
  recordCount: number
  snapshotDate: string | null
}
interface FinancialTrendRow {
  period: string
  metric: string
  currency: string
  totalAmount: number
  recordCount: number
}
interface LoanReportRow {
  id: string
  currentBalance: number
  principalAmount: number
  currency: string
}
interface TransferReportRow {
  transferDate: string
  amountSent: number
  currencySent: string
  transferFee: number | null
  exchangeRate: number
}

export default function ExportReportsPage() {
  const { accessToken, isLoading: authLoading } = useAuth()
  const [reportData, setReportData] = useState<ReportData | null>(null)
  const [loading, setLoading] = useState(true)
  const [generating, setGenerating] = useState(false)
  const [selectedReportType, setSelectedReportType] = useState("monthly")
  const [selectedFormat, setSelectedFormat] = useState("pdf")
  const [dateFrom, setDateFrom] = useState<Date>()
  const [dateTo, setDateTo] = useState<Date>()

  const generateReport = useCallback(async () => {
    if (authLoading) return
    if (!accessToken) {
      setLoading(false)
      setReportData(null)
      return
    }
    setLoading(true)
    try {
      let startDate: Date
      let endDate = new Date()
      switch (selectedReportType) {
        case "monthly":
          startDate = new Date(endDate.getFullYear(), endDate.getMonth(), 1)
          break
        case "quarterly": {
          const quarter = Math.floor(endDate.getMonth() / 3)
          startDate = new Date(endDate.getFullYear(), quarter * 3, 1)
          break
        }
        case "yearly":
          startDate = new Date(endDate.getFullYear(), 0, 1)
          break
        case "custom":
          startDate = dateFrom || new Date(endDate.getFullYear(), 0, 1)
          endDate = dateTo || endDate
          break
        default:
          startDate = new Date(endDate.getFullYear(), endDate.getMonth(), 1)
      }
      const asLocalDate = (date: Date) => {
        const year = date.getFullYear()
        const month = String(date.getMonth() + 1).padStart(2, "0")
        const day = String(date.getDate()).padStart(2, "0")
        return `${year}-${month}-${day}`
      }
      const query = `?from=${asLocalDate(startDate)}&to=${asLocalDate(endDate)}`
      const [summaryRows, trendRows, loanPage, transferPage] = await Promise.all([
        apiRequest<FinancialReportRow[]>(`${API.reportsSummary}${query}`, accessToken),
        apiRequest<FinancialTrendRow[]>(`${API.analyticsMonthlyTrends}${query}`, accessToken),
        apiGetPage<LoanReportRow>(`${API.loans}?page=0&size=1000&sort=createdAt,desc`, accessToken),
        apiGetPage<TransferReportRow>(`${API.transfers}?page=0&size=1000&sort=transferDate,desc`, accessToken),
      ])

      const incomeRows = summaryRows.filter((row) => row.metric === "INCOME" || row.metric === "TRANSACTION_INCOME")
      const expenseRows = summaryRows.filter((row) => !["INCOME", "TRANSACTION_INCOME", "LOAN_OUTSTANDING"].includes(row.metric))
      const totalIncome = incomeRows.reduce((sum, row) => sum + toUSD(Number(row.totalAmount), row.currency), 0)
      const totalExpenses = expenseRows.reduce((sum, row) => sum + toUSD(Number(row.totalAmount), row.currency), 0)
      const netIncome = totalIncome - totalExpenses

      const categoryMap = new Map<string, number>()
      summaryRows.filter((row) => row.metric !== "LOAN_OUTSTANDING").forEach((row) => {
        const category = row.category || "Other"
        categoryMap.set(category, (categoryMap.get(category) || 0) + toUSD(Number(row.totalAmount), row.currency))
      })
      const topCategories = Array.from(categoryMap.entries())
        .map(([category, amount]) => ({
          category,
          amount,
          percentage: totalExpenses > 0 ? (Math.abs(amount) / totalExpenses) * 100 : 0,
        }))
        .sort((left, right) => Math.abs(right.amount) - Math.abs(left.amount))
        .slice(0, 5)

      const totalDebt = loanPage.content.reduce((sum, loan) => sum + toUSD(Number(loan.currentBalance), loan.currency), 0)
      const totalPrincipal = loanPage.content.reduce((sum, loan) => sum + toUSD(Number(loan.principalAmount), loan.currency), 0)
      const totalPaid = Math.max(0, totalPrincipal - totalDebt)
      const averageProgress = totalPrincipal > 0 ? (totalPaid / totalPrincipal) * 100 : 0

      const transfersInRange = transferPage.content.filter((transfer) =>
        transfer.transferDate >= asLocalDate(startDate) && transfer.transferDate <= asLocalDate(endDate)
      )
      const totalSent = transfersInRange.reduce((sum, item) => sum + toUSD(Number(item.amountSent), item.currencySent), 0)
      const totalFees = transfersInRange.reduce((sum, item) => sum + toUSD(Number(item.transferFee ?? 0), item.currencySent), 0)
      const averageRate = transfersInRange.length > 0
        ? transfersInRange.reduce((sum, item) => sum + Number(item.exchangeRate), 0) / transfersInRange.length
        : 0

      const monthlyMap = new Map<string, { income: number; expenses: number }>()
      trendRows.forEach((row) => {
        const current = monthlyMap.get(row.period) ?? { income: 0, expenses: 0 }
        const amount = toUSD(Number(row.totalAmount), row.currency)
        if (row.metric === "INCOME" || row.metric === "TRANSACTION_INCOME") current.income += amount
        else current.expenses += amount
        monthlyMap.set(row.period, current)
      })
      const monthlyTrends = Array.from(monthlyMap.entries()).sort(([a], [b]) => a.localeCompare(b)).map(([period, values]) => ({
        month: new Date(`${period}-01T00:00:00`).toLocaleDateString("en-US", { month: "short" }),
        income: values.income,
        expenses: values.expenses,
      }))

      setReportData({
        totalIncome, totalExpenses, netIncome, topCategories, monthlyTrends,
        loanSummary: { totalDebt, totalPaid, averageProgress },
        transferSummary: { totalSent, totalFees, averageRate },
      })
    } catch (error) {
      console.error("Error generating report:", error)
      setReportData(null)
    } finally {
      setLoading(false)
    }
  }, [accessToken, authLoading, selectedReportType, dateFrom, dateTo])

  useEffect(() => {
    void generateReport()
  }, [generateReport])

  const handleExport = async () => {
    setGenerating(true)
    try {
      // Simulate export generation
      await new Promise((resolve) => setTimeout(resolve, 2000))

      // In a real app, you would generate the actual file here
      const filename = `financial-report-${format(new Date(), "yyyy-MM-dd")}.${selectedFormat}`

      // Create a simple CSV for demonstration
      if (selectedFormat === "csv") {
        const csvContent = [
          ["Category", "Amount", "Percentage"],
          ...reportData!.topCategories.map((cat) => [
            cat.category,
            cat.amount.toString(),
            cat.percentage.toFixed(2) + "%",
          ]),
        ]
          .map((row) => row.join(","))
          .join("\n")

        const blob = new Blob([csvContent], { type: "text/csv" })
        const url = window.URL.createObjectURL(blob)
        const a = document.createElement("a")
        a.href = url
        a.download = filename
        a.click()
        window.URL.revokeObjectURL(url)
      } else {
        // For PDF and Excel, show a message (in real app, implement actual generation)
        alert(`${selectedFormat.toUpperCase()} export would be generated here. Feature coming soon!`)
      }
    } catch (error) {
      console.error("Error exporting report:", error)
    } finally {
      setGenerating(false)
    }
  }

  if (loading) {
    return (
      <div className="space-y-6">
        <div>
          <h1 className="text-3xl font-bold tracking-tight">Export Reports</h1>
          <p className="text-muted-foreground">Generate and export comprehensive financial reports</p>
        </div>
        <div className="grid gap-6">
          {[...Array(3)].map((_, i) => (
            <Card key={i}>
              <CardHeader>
                <div className="h-6 bg-muted rounded w-32 animate-pulse" />
                <div className="h-4 bg-muted rounded w-48 animate-pulse" />
              </CardHeader>
              <CardContent>
                <div className="space-y-4">
                  <div className="h-10 bg-muted rounded animate-pulse" />
                  <div className="h-10 bg-muted rounded animate-pulse" />
                </div>
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
        <h1 className="text-3xl font-bold tracking-tight">Export Reports</h1>
        <p className="text-muted-foreground">Generate and export comprehensive financial reports</p>
      </div>

      {/* Report Configuration */}
      <Card>
        <CardHeader>
          <CardTitle>Report Configuration</CardTitle>
          <CardDescription>Configure your report parameters and export format</CardDescription>
        </CardHeader>
        <CardContent>
          <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
            <div className="space-y-2">
              <label className="text-sm font-medium">Report Type</label>
              <Select value={selectedReportType} onValueChange={setSelectedReportType}>
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {reportTypes.map((type) => (
                    <SelectItem key={type.value} value={type.value}>
                      {type.label}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            <div className="space-y-2">
              <label className="text-sm font-medium">Export Format</label>
              <Select value={selectedFormat} onValueChange={setSelectedFormat}>
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {exportFormats.map((format) => (
                    <SelectItem key={format.value} value={format.value}>
                      <div className="flex items-center gap-2">
                        <format.icon className="h-4 w-4" />
                        {format.label}
                      </div>
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            {selectedReportType === "custom" && (
              <>
                <div className="space-y-2">
                  <label className="text-sm font-medium">From Date</label>
                  <Popover>
                    <PopoverTrigger asChild>
                      <Button
                        variant="outline"
                        className={cn(
                          "w-full justify-start text-left font-normal",
                          !dateFrom && "text-muted-foreground",
                        )}
                      >
                        <CalendarIcon className="mr-2 h-4 w-4" />
                        {dateFrom ? format(dateFrom, "PPP") : "Pick a date"}
                      </Button>
                    </PopoverTrigger>
                    <PopoverContent className="w-auto p-0">
                      <Calendar mode="single" selected={dateFrom} onSelect={setDateFrom} initialFocus />
                    </PopoverContent>
                  </Popover>
                </div>

                <div className="space-y-2">
                  <label className="text-sm font-medium">To Date</label>
                  <Popover>
                    <PopoverTrigger asChild>
                      <Button
                        variant="outline"
                        className={cn("w-full justify-start text-left font-normal", !dateTo && "text-muted-foreground")}
                      >
                        <CalendarIcon className="mr-2 h-4 w-4" />
                        {dateTo ? format(dateTo, "PPP") : "Pick a date"}
                      </Button>
                    </PopoverTrigger>
                    <PopoverContent className="w-auto p-0">
                      <Calendar mode="single" selected={dateTo} onSelect={setDateTo} initialFocus />
                    </PopoverContent>
                  </Popover>
                </div>
              </>
            )}
          </div>

          <div className="flex justify-end mt-6">
            <Button onClick={handleExport} disabled={generating}>
              {generating ? (
                <>
                  <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-white mr-2" />
                  Generating...
                </>
              ) : (
                <>
                  <Download className="mr-2 h-4 w-4" />
                  Export Report
                </>
              )}
            </Button>
          </div>
        </CardContent>
      </Card>

      {reportData && (
        <>
          {/* Financial Summary */}
          <div className="grid gap-4 md:grid-cols-3">
            <Card>
              <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                <CardTitle className="text-sm font-medium">Total Income</CardTitle>
                <TrendingUp className="h-4 w-4 text-muted-foreground" />
              </CardHeader>
              <CardContent>
                <div className="text-2xl font-bold text-green-600">{formatUSDDisplay(toUSD(reportData.totalIncome, "USD"))}</div>
              </CardContent>
            </Card>

            <Card>
              <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                <CardTitle className="text-sm font-medium">Total Expenses</CardTitle>
                <TrendingDown className="h-4 w-4 text-muted-foreground" />
              </CardHeader>
              <CardContent>
                <div className="text-2xl font-bold text-red-600">{formatUSDDisplay(toUSD(reportData.totalExpenses, "USD"))}</div>
              </CardContent>
            </Card>

            <Card>
              <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                <CardTitle className="text-sm font-medium">Net Income</CardTitle>
                <BarChart3 className="h-4 w-4 text-muted-foreground" />
              </CardHeader>
              <CardContent>
            <div className={`text-2xl font-bold ${reportData.netIncome >= 0 ? "text-green-600" : "text-red-600"}`}>
              {formatUSDDisplay(toUSD(reportData.netIncome, "USD"))}
            </div>
              </CardContent>
            </Card>
          </div>

          {/* Category Breakdown */}
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <PieChart className="h-5 w-5" />
                Category Breakdown
              </CardTitle>
              <CardDescription>Top spending and earning categories</CardDescription>
            </CardHeader>
            <CardContent>
              <div className="space-y-4">
                {reportData.topCategories.map((category, index) => (
                  <div key={category.category} className="space-y-2">
                    <div className="flex justify-between items-center">
                      <span className="font-medium">{category.category}</span>
                      <div className="text-right">
                        <span className={`font-semibold ${category.amount >= 0 ? "text-green-600" : "text-red-600"}`}>
                          {formatUSDDisplay(toUSD(category.amount, "USD"))}
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

          {/* Loan Summary */}
          <div className="grid gap-4 md:grid-cols-2">
            <Card>
              <CardHeader>
                <CardTitle>Loan Summary</CardTitle>
                <CardDescription>Overview of your debt situation</CardDescription>
              </CardHeader>
              <CardContent className="space-y-4">
                <div className="flex justify-between items-center">
                  <span>Total Debt Remaining</span>
                  <span className="font-semibold text-red-600">{formatUSDDisplay(toUSD(reportData.loanSummary.totalDebt, "USD"))}</span>
                </div>
                <div className="flex justify-between items-center">
                  <span>Total Paid Off</span>
                  <span className="font-semibold text-green-600">
                    {formatUSDDisplay(toUSD(reportData.transferSummary.totalSent, "USD"))}
                  </span>
                </div>
                <div className="space-y-2">
                  <div className="flex justify-between items-center">
                    <span>Average Progress</span>
                    <span className="font-semibold">{reportData.loanSummary.averageProgress.toFixed(1)}%</span>
                  </div>
                  <Progress value={reportData.loanSummary.averageProgress} className="h-2" />
                </div>
              </CardContent>
            </Card>

            <Card>
              <CardHeader>
                <CardTitle>International Transfers</CardTitle>
                <CardDescription>Summary of your international transactions</CardDescription>
              </CardHeader>
              <CardContent className="space-y-4">
                <div className="flex justify-between items-center">
                  <span>Total Sent</span>
                  <span className="font-semibold text-blue-600">
                    {formatCurrency(reportData.transferSummary.totalSent)}
                  </span>
                </div>
                <div className="flex justify-between items-center">
                  <span>Total Fees</span>
                  <span className="font-semibold text-orange-600">
                    {formatUSDDisplay(toUSD(reportData.transferSummary.totalFees, "USD"))}
                  </span>
                </div>
                <div className="flex justify-between items-center">
                  <span>Average Exchange Rate</span>
                  <span className="font-semibold">{reportData.transferSummary.averageRate.toFixed(4)}</span>
                </div>
              </CardContent>
            </Card>
          </div>
        </>
      )}
    </div>
  )
}
