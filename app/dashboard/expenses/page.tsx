"use client"

import { useState, useEffect } from "react"
import { supabase } from "@/lib/supabase"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogTrigger } from "@/components/ui/dialog"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Label } from "@/components/ui/label"
import { Textarea } from "@/components/ui/textarea"
import { Badge } from "@/components/ui/badge"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { ArrowLeft, ArrowRight, Plus, Trash2, TrendingDown } from "lucide-react"
import { toUSD, formatUSDDisplay, formatDate, getCurrentExchangeRate } from "@/lib/utils"
import { format, subMonths, addMonths, startOfMonth, endOfMonth } from "date-fns"

interface Expense {
  id: string
  category: string
  amount: number
  currency: string
  date: string
  description: string
  created_at: string
}

interface MonthlyStats {
  totalSpent: number
  budgetLimit: number
  spent: number
  remaining: number
}

const EXPENSE_CATEGORIES = [
  "Food & Dining",
  "Transportation",
  "Healthcare",
  "Entertainment",
  "Shopping",
  "Bills & Utilities",
  "Education",
  "Travel",
  "Other",
]

export default function ExpenseTrackerPage() {
  const [expenses, setExpenses] = useState<Expense[]>([])
  const [loading, setLoading] = useState(true)
  const [currentDate, setCurrentDate] = useState(new Date())
  const [monthlyStats, setMonthlyStats] = useState<MonthlyStats>({
    totalSpent: 0,
    budgetLimit: 5000, // Default monthly budget in USD
    spent: 0,
    remaining: 5000,
  })

  // Add Expense Dialog state
  const [isAddDialogOpen, setIsAddDialogOpen] = useState(false)
  const [currentExchangeRate, setCurrentExchangeRate] = useState("83")
  const [newExpense, setNewExpense] = useState({
    category: "Food & Dining",
    amount: "",
    currency: "USD",
    exchangeRate: "83",
    date: format(new Date(), "yyyy-MM-dd"),
    description: "",
  })

  // Load current exchange rate on mount
  useEffect(() => {
    const rate = getCurrentExchangeRate()
    setCurrentExchangeRate(rate.toString())
    setNewExpense((prev) => ({ ...prev, exchangeRate: rate.toString() }))
  }, [])

  // Fetch expenses for current month
  useEffect(() => {
    fetchExpensesForMonth()
  }, [currentDate])

  const fetchExpensesForMonth = async () => {
    setLoading(true)
    try {
      const start = startOfMonth(currentDate)
      const end = endOfMonth(currentDate)

      const { data, error } = await supabase
        .from("expenses")
        .select("*")
        .gte("date", format(start, "yyyy-MM-dd"))
        .lte("date", format(end, "yyyy-MM-dd"))
        .order("date", { ascending: false })

      if (error) throw error

      setExpenses(data || [])
      calculateMonthlyStats(data || [])
    } catch (error) {
      console.error("[v0] Error fetching expenses:", error)
      setExpenses([])
      calculateMonthlyStats([])
    } finally {
      setLoading(false)
    }
  }

  const calculateMonthlyStats = (expenseList: Expense[]) => {
    let totalSpent = 0
    expenseList.forEach((expense) => {
      totalSpent += toUSD(expense.amount, expense.currency)
    })

    const budgetLimit = 5000 // Can be made dynamic per user
    setMonthlyStats({
      totalSpent,
      budgetLimit,
      spent: totalSpent,
      remaining: Math.max(0, budgetLimit - totalSpent),
    })
  }

  const handleAddExpense = async () => {
    if (!newExpense.amount || isNaN(parseFloat(newExpense.amount))) {
      alert("Please enter a valid amount")
      return
    }

    try {
      const amountInUSD =
        newExpense.currency === "INR"
          ? parseFloat(newExpense.amount) / parseFloat(newExpense.exchangeRate)
          : parseFloat(newExpense.amount)

      const { error } = await supabase.from("expenses").insert([
        {
          category: newExpense.category,
          amount: amountInUSD,
          currency: "USD",
          date: newExpense.date,
          description: newExpense.description,
        },
      ])

      if (error) throw error

      // Reset form and refresh
      setNewExpense({
        category: "Food & Dining",
        amount: "",
        currency: "USD",
        exchangeRate: currentExchangeRate,
        date: format(new Date(), "yyyy-MM-dd"),
        description: "",
      })
      setIsAddDialogOpen(false)
      fetchExpensesForMonth()
    } catch (error) {
      console.error("[v0] Error adding expense:", error)
      alert("Failed to add expense")
    }
  }

  const handleDeleteExpense = async (id: string) => {
    if (!confirm("Are you sure you want to delete this expense?")) return

    try {
      const { error } = await supabase.from("expenses").delete().eq("id", id)

      if (error) throw error

      setExpenses(expenses.filter((e) => e.id !== id))
      fetchExpensesForMonth()
    } catch (error) {
      console.error("[v0] Error deleting expense:", error)
      alert("Failed to delete expense")
    }
  }

  const goToPreviousMonth = () => {
    setCurrentDate(subMonths(currentDate, 1))
  }

  const goToNextMonth = () => {
    setCurrentDate(addMonths(currentDate, 1))
  }

  const goToCurrentMonth = () => {
    setCurrentDate(new Date())
  }

  const isOverBudget = monthlyStats.spent > monthlyStats.budgetLimit
  const budgetPercentage = (monthlyStats.spent / monthlyStats.budgetLimit) * 100

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="space-y-2">
        <h1 className="text-3xl font-bold tracking-tight">Expense Management</h1>
        <p className="text-muted-foreground">Viewing: {format(currentDate, "MMMM yyyy")}</p>
      </div>

      {/* Month Navigation */}
      <div className="flex items-center justify-between gap-4">
        <div className="flex gap-2">
          <Button variant="outline" size="sm" onClick={goToPreviousMonth}>
            <ArrowLeft className="h-4 w-4 mr-2" />
            Previous
          </Button>
          <Button variant="outline" size="sm" onClick={goToCurrentMonth}>
            Current
          </Button>
          <Button variant="outline" size="sm" onClick={goToNextMonth}>
            Next
            <ArrowRight className="h-4 w-4 ml-2" />
          </Button>
        </div>

        <Dialog open={isAddDialogOpen} onOpenChange={setIsAddDialogOpen}>
          <DialogTrigger asChild>
            <Button size="sm" className="gap-2">
              <Plus className="h-4 w-4" />
              Add Expense
            </Button>
          </DialogTrigger>
          <DialogContent className="max-w-md">
            <DialogHeader>
              <DialogTitle>Add New Expense</DialogTitle>
            </DialogHeader>
            <div className="space-y-4">
              {/* Category */}
              <div className="space-y-2">
                <Label>Category</Label>
                <Select
                  value={newExpense.category}
                  onValueChange={(v) => setNewExpense({ ...newExpense, category: v })}
                >
                  <SelectTrigger>
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    {EXPENSE_CATEGORIES.map((cat) => (
                      <SelectItem key={cat} value={cat}>
                        {cat}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>

              {/* Amount & Currency */}
              <div className="grid grid-cols-3 gap-2">
                <div className="space-y-2">
                  <Label>Amount</Label>
                  <Input
                    type="number"
                    step="0.01"
                    placeholder="0.00"
                    value={newExpense.amount}
                    onChange={(e) => setNewExpense({ ...newExpense, amount: e.target.value })}
                  />
                </div>
                <div className="space-y-2">
                  <Label>Currency</Label>
                  <Select
                    value={newExpense.currency}
                    onValueChange={(v) => setNewExpense({ ...newExpense, currency: v })}
                  >
                    <SelectTrigger>
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="USD">USD</SelectItem>
                      <SelectItem value="INR">INR</SelectItem>
                    </SelectContent>
                  </Select>
                </div>
              </div>

              {/* Exchange Rate (if INR) */}
              {newExpense.currency === "INR" && (
                <div className="space-y-2">
                  <Label>Exchange Rate (INR to USD)</Label>
                  <Input
                    type="number"
                    step="0.01"
                    placeholder="83"
                    value={newExpense.exchangeRate}
                    onChange={(e) => setNewExpense({ ...newExpense, exchangeRate: e.target.value })}
                  />
                  <p className="text-xs text-muted-foreground">
                    1 INR = {(1 / parseFloat(newExpense.exchangeRate || "83")).toFixed(4)} USD
                  </p>
                </div>
              )}

              {/* Date */}
              <div className="space-y-2">
                <Label>Date</Label>
                <Input
                  type="date"
                  value={newExpense.date}
                  onChange={(e) => setNewExpense({ ...newExpense, date: e.target.value })}
                />
              </div>

              {/* Description */}
              <div className="space-y-2">
                <Label>Description (Optional)</Label>
                <Textarea
                  placeholder="Add notes..."
                  value={newExpense.description}
                  onChange={(e) => setNewExpense({ ...newExpense, description: e.target.value })}
                  className="resize-none"
                  rows={3}
                />
              </div>

              {/* Submit Button */}
              <Button onClick={handleAddExpense} className="w-full">
                Add Expense
              </Button>
            </div>
          </DialogContent>
        </Dialog>
      </div>

      {/* Stats Cards */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Total Spent</CardTitle>
            <TrendingDown className="h-4 w-4 text-red-500" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold text-red-600">{formatUSDDisplay(monthlyStats.spent)}</div>
            <p className="text-xs text-muted-foreground mt-1">
              {expenses.length} transaction{expenses.length !== 1 ? "s" : ""}
            </p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Budget</CardTitle>
            <div className="h-4 w-4 rounded-full bg-blue-500" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold text-blue-600">{formatUSDDisplay(monthlyStats.budgetLimit)}</div>
            <p className="text-xs text-muted-foreground mt-1">Monthly budget limit</p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Budget Remaining</CardTitle>
            <div className={`h-4 w-4 rounded-full ${isOverBudget ? "bg-red-500" : "bg-green-500"}`} />
          </CardHeader>
          <CardContent>
            <div className={`text-2xl font-bold ${isOverBudget ? "text-red-600" : "text-green-600"}`}>
              {isOverBudget ? "-" : ""}
              {formatUSDDisplay(monthlyStats.remaining)}
            </div>
            {isOverBudget && <p className="text-xs text-red-600 mt-1">Over budget</p>}
          </CardContent>
        </Card>
      </div>

      {/* Budget Progress Bar */}
      <Card>
        <CardHeader>
          <CardTitle className="text-sm font-medium">Budget Progress</CardTitle>
          <div className="mt-2 flex items-end gap-2">
            <div className="flex-1 h-2 bg-muted rounded-full overflow-hidden">
              <div
                className={`h-full ${isOverBudget ? "bg-red-500" : "bg-green-500"}`}
                style={{ width: `${Math.min(budgetPercentage, 100)}%` }}
              />
            </div>
            <span className="text-xs font-semibold text-muted-foreground">{budgetPercentage.toFixed(1)}%</span>
          </div>
        </CardHeader>
      </Card>

      {/* Expenses Table */}
      <Card>
        <CardHeader>
          <CardTitle>Expenses</CardTitle>
          <CardDescription>{format(currentDate, "MMMM yyyy")} transactions</CardDescription>
        </CardHeader>
        <CardContent>
          {loading ? (
            <div className="text-center py-8 text-muted-foreground">Loading expenses...</div>
          ) : expenses.length === 0 ? (
            <div className="text-center py-8">
              <TrendingDown className="h-8 w-8 mx-auto mb-2 text-muted-foreground" />
              <h3 className="mt-2 text-sm font-semibold text-foreground">No expenses recorded</h3>
              <p className="mt-1 text-sm text-muted-foreground">
                Start tracking your expenses for {format(currentDate, "MMMM yyyy")}
              </p>
            </div>
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Date</TableHead>
                  <TableHead>Category</TableHead>
                  <TableHead>Description</TableHead>
                  <TableHead className="text-right">Amount (USD)</TableHead>
                  <TableHead className="w-10"></TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {expenses.map((expense) => (
                  <TableRow key={expense.id}>
                    <TableCell className="font-medium">{formatDate(new Date(expense.date))}</TableCell>
                    <TableCell>
                      <Badge variant="outline">{expense.category}</Badge>
                    </TableCell>
                    <TableCell className="text-sm text-muted-foreground">{expense.description || "-"}</TableCell>
                    <TableCell className="text-right font-semibold text-red-600">
                      {formatUSDDisplay(toUSD(expense.amount, expense.currency))}
                    </TableCell>
                    <TableCell>
                      <Button
                        variant="ghost"
                        size="sm"
                        onClick={() => handleDeleteExpense(expense.id)}
                        className="h-8 w-8 p-0"
                      >
                        <Trash2 className="h-4 w-4 text-destructive" />
                      </Button>
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
