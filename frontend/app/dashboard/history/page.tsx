"use client"

export const dynamic = "force-dynamic"

import { useEffect, useState } from "react"
import { useAuth } from "@/components/auth/auth-provider"
import { API, apiGetPage } from "@/lib/api"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Badge } from "@/components/ui/badge"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { Button } from "@/components/ui/button"
import { Calendar } from "@/components/ui/calendar"
import { Popover, PopoverContent, PopoverTrigger } from "@/components/ui/popover"
import { Search, Filter, Download, CalendarIcon, TrendingUp, TrendingDown } from "lucide-react"
import { formatDate, cn, toUSD, formatUSDDisplay } from "@/lib/utils"
import { format } from "date-fns"

interface Transaction {
  id: string
  type: string
  category: string
  amount: number
  currency: string
  description: string | null
  date: string
  source?: string
  recipient?: string | null
  status?: string
}

const transactionTypes = ["All", "Income", "Expense", "Transfer", "Loan Payment", "International Transfer"]

const categories = [
  "All",
  "Salary",
  "Freelance",
  "Business",
  "Investment",
  "Utilities",
  "Rent",
  "Food",
  "Transportation",
  "Healthcare",
  "Entertainment",
  "Other",
]

type IncomeHistoryRow = { id: string; category: string | null; amount: number; currency: string | null; description: string | null; date: string; source: string }
type TransferHistoryRow = { id: string; amountSent: number; currencySent: string; recipientName: string; recipientCountry: string; transferDate: string; status: string }
type LoanHistoryRow = { id: string; loanName: string }
type LoanPaymentHistoryRow = { id: string; loanId: string; amount: number; currency: string; paymentDate: string }
type ScheduledPaymentHistoryRow = { id: string; category: string | null; amount: number; currency: string; paymentName: string; dueDate: string; recipient: string | null; status: string }
type MonthlyExpenseHistoryRow = { id: string; category: string | null; amount: number; currency: string; expenseName: string; expenseDate: string; notes: string | null }

export default function TransactionHistoryPage() {
  const { accessToken, isLoading: authLoading } = useAuth()
  const [transactions, setTransactions] = useState<Transaction[]>([])
  const [filteredTransactions, setFilteredTransactions] = useState<Transaction[]>([])
  const [loading, setLoading] = useState(true)
  const [searchTerm, setSearchTerm] = useState("")
  const [selectedType, setSelectedType] = useState("All")
  const [selectedCategory, setSelectedCategory] = useState("All")
  const [dateFrom, setDateFrom] = useState<Date>()
  const [dateTo, setDateTo] = useState<Date>()

  useEffect(() => {
    if (!authLoading) void fetchTransactions()
  }, [authLoading, accessToken])

  useEffect(() => {
    let filtered = [...transactions]

    // Filter by search term
    if (searchTerm) {
      filtered = filtered.filter(
        (transaction) =>
          transaction.description?.toLowerCase().includes(searchTerm.toLowerCase()) ||
          transaction.category.toLowerCase().includes(searchTerm.toLowerCase()) ||
          transaction.source?.toLowerCase().includes(searchTerm.toLowerCase()) ||
          transaction.recipient?.toLowerCase().includes(searchTerm.toLowerCase()),
      )
    }

    // Filter by type
    if (selectedType !== "All") {
      filtered = filtered.filter((transaction) => transaction.type === selectedType)
    }

    // Filter by category
    if (selectedCategory !== "All") {
      filtered = filtered.filter((transaction) => transaction.category === selectedCategory)
    }

    // Filter by date range
    if (dateFrom) {
      filtered = filtered.filter((transaction) => new Date(transaction.date) >= dateFrom)
    }
    if (dateTo) {
      filtered = filtered.filter((transaction) => new Date(transaction.date) <= dateTo)
    }

    setFilteredTransactions(filtered)
  }, [transactions, searchTerm, selectedType, selectedCategory, dateFrom, dateTo])

  const fetchTransactions = async () => {
    if (!accessToken) {
      setLoading(false)
      return
    }
    try {
      const [incomePage, transferPage, loanPage, scheduledPage, expensePage] = await Promise.all([
        apiGetPage<IncomeHistoryRow>(`${API.income}?page=0&size=1000&sort=date,desc`, accessToken),
        apiGetPage<TransferHistoryRow>(`${API.transfers}?page=0&size=1000&sort=transferDate,desc`, accessToken),
        apiGetPage<LoanHistoryRow>(`${API.loans}?page=0&size=1000&sort=createdAt,desc`, accessToken),
        apiGetPage<ScheduledPaymentHistoryRow>(`${API.scheduledPayments}?page=0&size=1000&sort=dueDate,desc`, accessToken),
        apiGetPage<MonthlyExpenseHistoryRow>(`${API.monthlyExpenses}?page=0&size=1000&sort=expenseDate,desc`, accessToken),
      ])

      // Loan payments are nested under their parent loan so the API can enforce
      // ownership of both resources. Fetch each loan's page and merge locally.
      const paymentGroups = await Promise.all(loanPage.content.map(async (loan) => {
        const page = await apiGetPage<LoanPaymentHistoryRow>(
          `${API.loans}/${loan.id}/payments?page=0&size=1000&sort=paymentDate,desc`,
          accessToken
        )
        return page.content.map((payment) => ({ ...payment, loanName: loan.loanName }))
      }))

      const allTransactions: Transaction[] = []
      incomePage.content.forEach((income) => allTransactions.push({
        id: income.id, type: "Income", category: income.category || "Income",
        amount: Number(income.amount), currency: income.currency || "USD",
        description: income.description, date: income.date, source: income.source,
      }))
      transferPage.content.forEach((transfer) => allTransactions.push({
        id: transfer.id, type: "International Transfer", category: "Transfer",
        amount: -Number(transfer.amountSent), currency: transfer.currencySent,
        description: `Transfer to ${transfer.recipientName} in ${transfer.recipientCountry}`,
        date: transfer.transferDate, recipient: transfer.recipientName, status: transfer.status,
      }))
      paymentGroups.flat().forEach((payment) => allTransactions.push({
        id: payment.id, type: "Loan Payment", category: "Loan Payment",
        amount: -Number(payment.amount), currency: payment.currency || "INR",
        description: `Payment for ${payment.loanName || "Loan"}`, date: payment.paymentDate,
      }))
      scheduledPage.content.filter((payment) => payment.status === "paid").forEach((payment) => allTransactions.push({
        id: payment.id, type: "Expense", category: payment.category || "Other",
        amount: -Number(payment.amount), currency: payment.currency,
        description: payment.paymentName, date: payment.dueDate, recipient: payment.recipient,
      }))
      expensePage.content.forEach((expense) => allTransactions.push({
        id: expense.id, type: "Expense", category: expense.category || "Other",
        amount: -Number(expense.amount), currency: expense.currency,
        description: expense.notes || expense.expenseName, date: expense.expenseDate,
      }))

      allTransactions.sort((left, right) => new Date(right.date).getTime() - new Date(left.date).getTime())
      setTransactions(allTransactions)
    } catch (error) {
      console.error("Error fetching transactions:", error)
    } finally {
      setLoading(false)
    }
  }

  const totalIncome = filteredTransactions.filter((t) => t.amount > 0).reduce((sum, t) => sum + t.amount, 0)

  const totalExpenses = Math.abs(filteredTransactions.filter((t) => t.amount < 0).reduce((sum, t) => sum + t.amount, 0))

  const netAmount = totalIncome - totalExpenses

  if (loading) {
    return (
      <div className="space-y-6">
        <div>
          <h1 className="text-3xl font-bold tracking-tight">Transaction History</h1>
          <p className="text-muted-foreground">View and filter your financial transactions</p>
        </div>
        <div className="grid gap-4 md:grid-cols-3">
          {[...Array(3)].map((_, i) => (
            <Card key={i}>
              <CardHeader>
                <div className="h-4 bg-muted rounded w-20 animate-pulse" />
              </CardHeader>
              <CardContent>
                <div className="h-8 bg-muted rounded w-24 animate-pulse" />
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
        <h1 className="text-3xl font-bold tracking-tight">Transaction History</h1>
        <p className="text-muted-foreground">View and filter your financial transactions</p>
      </div>

      {/* Summary Cards */}
      <div className="grid gap-4 md:grid-cols-4">
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Total Income</CardTitle>
            <TrendingUp className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold text-green-600">{formatUSDDisplay(toUSD(totalIncome, "USD"))}</div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Total Expenses</CardTitle>
            <TrendingDown className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold text-red-600">{formatUSDDisplay(toUSD(totalExpenses, "USD"))}</div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Net Amount</CardTitle>
            <TrendingUp className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className={`text-2xl font-bold ${netAmount >= 0 ? "text-green-600" : "text-red-600"}`}>
              {formatUSDDisplay(toUSD(netAmount, "USD"))}
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Transactions</CardTitle>
            <Filter className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{filteredTransactions.length}</div>
          </CardContent>
        </Card>
      </div>

      {/* Filters */}
      <Card>
        <CardHeader>
          <CardTitle>Filters</CardTitle>
          <CardDescription>Filter transactions by various criteria</CardDescription>
        </CardHeader>
        <CardContent>
          <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-5">
            <div className="space-y-2">
              <Label htmlFor="search">Search</Label>
              <div className="relative">
                <Search className="absolute left-2 top-2.5 h-4 w-4 text-muted-foreground" />
                <Input
                  id="search"
                  placeholder="Search transactions..."
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                  className="pl-8"
                />
              </div>
            </div>

            <div className="space-y-2">
              <Label>Type</Label>
              <Select value={selectedType} onValueChange={setSelectedType}>
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {transactionTypes.map((type) => (
                    <SelectItem key={type} value={type}>
                      {type}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            <div className="space-y-2">
              <Label>Category</Label>
              <Select value={selectedCategory} onValueChange={setSelectedCategory}>
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {categories.map((category) => (
                    <SelectItem key={category} value={category}>
                      {category}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            <div className="space-y-2">
              <Label>From Date</Label>
              <Popover>
                <PopoverTrigger asChild>
                  <Button
                    variant="outline"
                    className={cn("w-full justify-start text-left font-normal", !dateFrom && "text-muted-foreground")}
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
              <Label>To Date</Label>
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
          </div>

          <div className="flex justify-between items-center mt-4">
            <Button
              variant="outline"
              onClick={() => {
                setSearchTerm("")
                setSelectedType("All")
                setSelectedCategory("All")
                setDateFrom(undefined)
                setDateTo(undefined)
              }}
            >
              Clear Filters
            </Button>
            <Button variant="outline">
              <Download className="mr-2 h-4 w-4" />
              Export CSV
            </Button>
          </div>
        </CardContent>
      </Card>

      {/* Transactions Table */}
      <Card>
        <CardHeader>
          <CardTitle>Transactions</CardTitle>
          <CardDescription>
            Showing {filteredTransactions.length} of {transactions.length} transactions
          </CardDescription>
        </CardHeader>
        <CardContent>
          {filteredTransactions.length === 0 ? (
            <div className="text-center py-8">
              <Search className="mx-auto h-12 w-12 text-muted-foreground" />
              <h3 className="mt-2 text-sm font-semibold text-foreground">No transactions found</h3>
              <p className="mt-1 text-sm text-muted-foreground">Try adjusting your filters or search terms.</p>
            </div>
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Date</TableHead>
                  <TableHead>Type</TableHead>
                  <TableHead>Description</TableHead>
                  <TableHead>Category</TableHead>
                  <TableHead>Amount</TableHead>
                  <TableHead>Status</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {filteredTransactions.map((transaction) => (
                  <TableRow key={transaction.id}>
                    <TableCell>{formatDate(transaction.date)}</TableCell>
                    <TableCell>
                      <Badge variant="outline">{transaction.type}</Badge>
                    </TableCell>
                    <TableCell>
                      <div>
                        <p className="font-medium">{transaction.description || "—"}</p>
                        {transaction.source && (
                          <p className="text-sm text-muted-foreground">From: {transaction.source}</p>
                        )}
                        {transaction.recipient && (
                          <p className="text-sm text-muted-foreground">To: {transaction.recipient}</p>
                        )}
                      </div>
                    </TableCell>
                    <TableCell>
                      <Badge variant="secondary">{transaction.category}</Badge>
                    </TableCell>
                    <TableCell>
                      <span className={`font-semibold ${transaction.amount > 0 ? "text-green-600" : "text-red-600"}`}>
                        {transaction.amount > 0 ? "+" : ""}
                        {formatUSDDisplay(toUSD(transaction.amount, transaction.currency))}
                      </span>
                    </TableCell>
                    <TableCell>
                      {transaction.status && (
                        <Badge
                          variant={
                            transaction.status === "completed"
                              ? "default"
                              : transaction.status === "pending"
                                ? "secondary"
                                : "destructive"
                          }
                        >
                          {transaction.status}
                        </Badge>
                      )}
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}
        </CardContent>
      </Card>
    </div>
  )
}
