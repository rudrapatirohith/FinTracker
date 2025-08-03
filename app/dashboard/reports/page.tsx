"use client"

import { useEffect, useState } from "react"
import { supabase } from "@/lib/supabase"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Calendar } from "@/components/ui/calendar"
import { Popover, PopoverContent, PopoverTrigger } from "@/components/ui/popover"
import { Badge } from "@/components/ui/badge"
import { Progress } from "@/components/ui/progress"
import { Download, FileText, CalendarIcon, TrendingUp, TrendingDown, PieChart, BarChart3 } from "lucide-react"
import { formatCurrency, cn } from "@/lib/utils"
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

export default function ExportReportsPage() {
  const [reportData, setReportData] = useState<ReportData | null>(null)
  const [loading, setLoading] = useState(true)
  const [generating, setGenerating] = useState(false)
  const [selectedReportType, setSelectedReportType] = useState("monthly")
  const [selectedFormat, setSelectedFormat] = useState("pdf")
  const [dateFrom, setDateFrom] = useState<Date>()
  const [dateTo, setDateTo] = useState<Date>()

  useEffect(() => {
    generateReport()
  }, [selectedReportType, dateFrom, dateTo])

  const generateReport = async () => {
    setLoading(true)
    try {
      const {
        data: { user },
      } = await supabase.auth.getUser()
      if (!user) return

      // Calculate date range based on report type
      let startDate: Date
      let endDate = new Date()

      switch (selectedReportType) {
        case "monthly":
          startDate = new Date(endDate.getFullYear(), endDate.getMonth(), 1)
          break
        case "quarterly":
          const quarter = Math.floor(endDate.getMonth() / 3)
          startDate = new Date(endDate.getFullYear(), quarter * 3, 1)
          break
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

      // Fetch data from all relevant tables
      const [incomeData, transferData, loanData, loanPaymentData, scheduledPaymentData] = await Promise.all([
        supabase
          .from("income")
          .select("*")
          .eq("user_id", user.id)
          .gte("date", startDate.toISOString().split("T")[0])
          .lte("date", endDate.toISOString().split("T")[0]),
        supabase
          .from("international_transfers")
          .select("*")
          .eq("user_id", user.id)
          .gte("transfer_date", startDate.toISOString().split("T")[0])
          .lte("transfer_date", endDate.toISOString().split("T")[0]),
        supabase.from("loans").select("*").eq("user_id", user.id),
        supabase
          .from("loan_payments")
          .select("*")
          .eq("user_id", user.id)
          .gte("payment_date", startDate.toISOString().split("T")[0])
          .lte("payment_date", endDate.toISOString().split("T")[0]),
        supabase
          .from("scheduled_payments")
          .select("*")
          .eq("user_id", user.id)
          .eq("status", "paid")
          .gte("due_date", startDate.toISOString().split("T")[0])
          .lte("due_date", endDate.toISOString().split("T")[0]),
      ])

      // Calculate totals
      const totalIncome = incomeData.data?.reduce((sum, item) => sum + Number(item.amount), 0) || 0
      const totalTransferFees = transferData.data?.reduce((sum, item) => sum + Number(item.transfer_fee), 0) || 0
      const totalLoanPayments = loanPaymentData.data?.reduce((sum, item) => sum + Number(item.amount), 0) || 0
      const totalScheduledPayments = scheduledPaymentData.data?.reduce((sum, item) => sum + Number(item.amount), 0) || 0
      const totalTransfersSent = transferData.data?.reduce((sum, item) => sum + Number(item.amount_sent), 0) || 0

      const totalExpenses = totalTransferFees + totalLoanPayments + totalScheduledPayments + totalTransfersSent
      const netIncome = totalIncome - totalExpenses

      // Calculate category breakdown
      const categoryMap = new Map<string, number>()

      // Add income categories
      incomeData.data?.forEach((item) => {
        const category = item.category || "Other Income"
        categoryMap.set(category, (categoryMap.get(category) || 0) + Number(item.amount))
      })

      // Add expense categories
      transferData.data?.forEach((item) => {
        categoryMap.set(
          "International Transfers",
          (categoryMap.get("International Transfers") || 0) + Number(item.amount_sent),
        )
      })

      loanPaymentData.data?.forEach((item) => {
        categoryMap.set("Loan Payments", (categoryMap.get("Loan Payments") || 0) + Number(item.amount))
      })

      scheduledPaymentData.data?.forEach((item) => {
        const category = item.category || "Other Expenses"
        categoryMap.set(category, (categoryMap.get(category) || 0) + Number(item.amount))
      })

      const topCategories = Array.from(categoryMap.entries())
        .map(([category, amount]) => ({
          category,
          amount,
          percentage: totalIncome > 0 ? (Math.abs(amount) / totalIncome) * 100 : 0,
        }))
        .sort((a, b) => Math.abs(b.amount) - Math.abs(a.amount))
        .slice(0, 5)

      // Calculate loan summary
      const totalDebt = loanData.data?.reduce((sum, loan) => sum + Number(loan.current_balance), 0) || 0
      const totalPrincipal = loanData.data?.reduce((sum, loan) => sum + Number(loan.principal_amount), 0) || 0
      const totalPaid = totalPrincipal - totalDebt
      const averageProgress = totalPrincipal > 0 ? (totalPaid / totalPrincipal) * 100 : 0

      // Calculate transfer summary
      const totalSent = transferData.data?.reduce((sum, item) => sum + Number(item.amount_sent), 0) || 0
      const totalFees = transferData.data?.reduce((sum, item) => sum + Number(item.transfer_fee), 0) || 0
      const averageRate =
        transferData.data?.length > 0
          ? transferData.data.reduce((sum, item) => sum + Number(item.exchange_rate), 0) / transferData.data.length
          : 0

      // Generate monthly trends (simplified for demo)
      const monthlyTrends = [
        { month: "Jan", income: totalIncome * 0.8, expenses: totalExpenses * 0.7 },
        { month: "Feb", income: totalIncome * 0.9, expenses: totalExpenses * 0.8 },
        { month: "Mar", income: totalIncome, expenses: totalExpenses },
      ]

      setReportData({
        totalIncome,
        totalExpenses,
        netIncome,
        topCategories,
        monthlyTrends,
        loanSummary: {
          totalDebt,
          totalPaid,
          averageProgress,
        },
        transferSummary: {
          totalSent,
          totalFees,
          averageRate,
        },
      })
    } catch (error) {
      console.error("Error generating report:", error)
    } finally {
      setLoading(false)
    }
  }

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
                <div className="text-2xl font-bold text-green-600">{formatCurrency(reportData.totalIncome)}</div>
              </CardContent>
            </Card>

            <Card>
              <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                <CardTitle className="text-sm font-medium">Total Expenses</CardTitle>
                <TrendingDown className="h-4 w-4 text-muted-foreground" />
              </CardHeader>
              <CardContent>
                <div className="text-2xl font-bold text-red-600">{formatCurrency(reportData.totalExpenses)}</div>
              </CardContent>
            </Card>

            <Card>
              <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                <CardTitle className="text-sm font-medium">Net Income</CardTitle>
                <BarChart3 className="h-4 w-4 text-muted-foreground" />
              </CardHeader>
              <CardContent>
                <div className={`text-2xl font-bold ${reportData.netIncome >= 0 ? "text-green-600" : "text-red-600"}`}>
                  {formatCurrency(reportData.netIncome)}
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
                  <span className="font-semibold text-red-600">{formatCurrency(reportData.loanSummary.totalDebt)}</span>
                </div>
                <div className="flex justify-between items-center">
                  <span>Total Paid Off</span>
                  <span className="font-semibold text-green-600">
                    {formatCurrency(reportData.loanSummary.totalPaid)}
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
                    {formatCurrency(reportData.transferSummary.totalFees)}
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
