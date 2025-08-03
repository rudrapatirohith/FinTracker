"use client"

import { useEffect, useState } from "react"
import { supabase } from "@/lib/supabase"
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
import { formatCurrency, formatDate, cn } from "@/lib/utils"
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
  recipient?: string
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

export default function TransactionHistoryPage() {
  const [transactions, setTransactions] = useState<Transaction[]>([])
  const [filteredTransactions, setFilteredTransactions] = useState<Transaction[]>([])
  const [loading, setLoading] = useState(true)
  const [searchTerm, setSearchTerm] = useState("")
  const [selectedType, setSelectedType] = useState("All")
  const [selectedCategory, setSelectedCategory] = useState("All")
  const [dateFrom, setDateFrom] = useState<Date>()
  const [dateTo, setDateTo] = useState<Date>()

  useEffect(() => {
    fetchTransactions()
  }, [])

  useEffect(() => {
    filterTransactions()
  }, [transactions, searchTerm, selectedType, selectedCategory, dateFrom, dateTo])

  const fetchTransactions = async () => {
    try {
      const {
        data: { user },
      } = await supabase.auth.getUser()
      if (!user) return

      // Fetch from multiple tables and combine
      const [incomeData, transferData, loanPaymentData, scheduledPaymentData] = await Promise.all([
        supabase.from("income").select("*").eq("user_id", user.id),
        supabase.from("international_transfers").select("*").eq("user_id", user.id),
        supabase.from("loan_payments").select("*, loans(loan_name)").eq("user_id", user.id),
        supabase.from("scheduled_payments").select("*").eq("user_id", user.id).eq("status", "paid"),
      ])

      const allTransactions: Transaction[] = []

      // Add income transactions
      if (incomeData.data) {
        incomeData.data.forEach((income) => {
          allTransactions.push({
            id: income.id,
            type: "Income",
            category: income.category || "Income",
            amount: income.amount,
            currency: income.currency || "USD",
            description: income.description,
            date: income.date,
            source: income.source,
          })
        })
      }

      // Add transfer transactions
      if (transferData.data) {
        transferData.data.forEach((transfer) => {
          allTransactions.push({
            id: transfer.id,
            type: "International Transfer",
            category: "Transfer",
            amount: -transfer.amount_sent, // Negative because it's money going out
            currency: transfer.currency_sent,
            description: `Transfer to ${transfer.recipient_name} in ${transfer.recipient_country}`,
            date: transfer.transfer_date,
            recipient: transfer.recipient_name,
            status: transfer.status,
          })
        })
      }

      // Add loan payment transactions
      if (loanPaymentData.data) {
        loanPaymentData.data.forEach((payment: any) => {
          allTransactions.push({
            id: payment.id,
            type: "Loan Payment",
            category: "Loan Payment",
            amount: -payment.amount, // Negative because it's money going out
            currency: "USD",
            description: `Payment for ${payment.loans?.loan_name || "Loan"}`,
            date: payment.payment_date,
          })
        })
      }

      // Add scheduled payments that were paid
      if (scheduledPaymentData.data) {
        scheduledPaymentData.data.forEach((payment) => {
          allTransactions.push({
            id: payment.id,
            type: "Expense",
            category: payment.category || "Other",
            amount: -payment.amount, // Negative because it's money going out
            currency: payment.currency,
            description: payment.payment_name,
            date: payment.due_date,
            recipient: payment.recipient,
          })
        })
      }

      // Sort by date (newest first)
      allTransactions.sort((a, b) => new Date(b.date).getTime() - new Date(a.date).getTime())

      setTransactions(allTransactions)
    } catch (error) {
      console.error("Error fetching transactions:", error)
    } finally {
      setLoading(false)
    }
  }

  const filterTransactions = () => {
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
            <div className="text-2xl font-bold text-green-600">{formatCurrency(totalIncome)}</div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Total Expenses</CardTitle>
            <TrendingDown className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold text-red-600">{formatCurrency(totalExpenses)}</div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Net Amount</CardTitle>
            <TrendingUp className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className={`text-2xl font-bold ${netAmount >= 0 ? "text-green-600" : "text-red-600"}`}>
              {formatCurrency(netAmount)}
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
              <h3 className="mt-2 text-sm font-semibold text-gray-900">No transactions found</h3>
              <p className="mt-1 text-sm text-gray-500">Try adjusting your filters or search terms.</p>
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
                        {formatCurrency(transaction.amount, transaction.currency)}
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
