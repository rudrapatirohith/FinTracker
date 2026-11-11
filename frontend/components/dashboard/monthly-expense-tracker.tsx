"use client"

import type React from "react"

import { useEffect, useState } from "react"
import { supabase, handleSupabaseError } from "@/lib/supabase"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Textarea } from "@/components/ui/textarea"
import { Badge } from "@/components/ui/badge"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { Alert, AlertDescription } from "@/components/ui/alert"
import { Progress } from "@/components/ui/progress"
import {
  Plus,
  Edit,
  Trash2,
  CreditCard,
  Calendar,
  TrendingUp,
  AlertCircle,
  IndianRupee,
  Info,
  RefreshCw,
  ShoppingCart,
} from "lucide-react"
import { formatUSD, formatINR, calculateINREquivalent, getCurrentExchangeRate, isValidExchangeRate } from "@/lib/utils"

interface MonthlyExpense {
  id: string
  expense_name: string
  amount: number
  currency: string
  exchange_rate: number | null
  inr_equivalent: number | null
  category: string | null
  expense_date: string
  payment_method: string | null
  notes: string | null
  created_at: string
}

const expenseCategories = [
  "Food & Dining",
  "Transportation",
  "Shopping",
  "Entertainment",
  "Bills & Utilities",
  "Healthcare",
  "Education",
  "Travel",
  "Groceries",
  "Rent",
  "Insurance",
  "Other",
]

const paymentMethods = ["Cash", "Credit Card", "Debit Card", "Bank Transfer", "Digital Wallet", "UPI", "Other"]

export default function MonthlyExpenseTracker() {
  const [expenses, setExpenses] = useState<MonthlyExpense[]>([])
  const [loading, setLoading] = useState(true)
  const [dialogOpen, setDialogOpen] = useState(false)
  const [editingExpense, setEditingExpense] = useState<MonthlyExpense | null>(null)
  const [error, setError] = useState("")
  const [success, setSuccess] = useState("")
  const [currentExchangeRate, setCurrentExchangeRate] = useState<number>(83.25)
  const [fetchingRate, setFetchingRate] = useState(false)
  const [monthlyBudget, setMonthlyBudget] = useState<number>(50000) // Default budget in INR

  const [formData, setFormData] = useState({
    expense_name: "",
    amount: "",
    currency: "INR",
    category: "",
    expense_date: new Date().toISOString().split("T")[0],
    payment_method: "",
    notes: "",
    exchange_rate: "",
  })

  useEffect(() => {
    fetchExpenses()
    fetchCurrentExchangeRate()
  }, [])

  const fetchCurrentExchangeRate = async () => {
    setFetchingRate(true)
    try {
      const rate = await getCurrentExchangeRate()
      setCurrentExchangeRate(rate)
      setFormData((prev) => ({ ...prev, exchange_rate: rate.toString() }))
    } catch (error) {
      console.error("Failed to fetch exchange rate:", error)
    } finally {
      setFetchingRate(false)
    }
  }

  const fetchExpenses = async () => {
    try {
      setError("")
      const {
        data: { user },
      } = await supabase.auth.getUser()

      if (!user) {
        setError("Please log in to view expenses")
        return
      }

      // Get current month's expenses
      const currentDate = new Date()
      const firstDayOfMonth = new Date(currentDate.getFullYear(), currentDate.getMonth(), 1)
      const lastDayOfMonth = new Date(currentDate.getFullYear(), currentDate.getMonth() + 1, 0)

      const { data, error } = await supabase
        .from("monthly_expenses")
        .select("*")
        .eq("user_id", user.id)
        .gte("expense_date", firstDayOfMonth.toISOString().split("T")[0])
        .lte("expense_date", lastDayOfMonth.toISOString().split("T")[0])
        .order("expense_date", { ascending: false })

      if (error) {
        handleSupabaseError(error, "fetch expenses")
        return
      }

      setExpenses(data || [])
    } catch (error: any) {
      console.error("Error fetching expenses:", error)
      setError(error.message || "Failed to fetch expenses")
    } finally {
      setLoading(false)
    }
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError("")
    setSuccess("")

    try {
      const {
        data: { user },
      } = await supabase.auth.getUser()

      if (!user) {
        setError("Please log in to add expenses")
        return
      }

      const amount = Number.parseFloat(formData.amount)
      let exchangeRate = null
      let inrEquivalent = null

      // Handle exchange rate for USD expenses
      if (formData.currency === "USD") {
        const rate = Number.parseFloat(formData.exchange_rate)
        if (!isValidExchangeRate(rate)) {
          setError("Please enter a valid exchange rate between 1 and 200")
          return
        }
        exchangeRate = rate
        inrEquivalent = calculateINREquivalent(amount, rate)
      } else {
        // For INR expenses, the amount is already in INR
        inrEquivalent = amount
      }

      const expenseData = {
        user_id: user.id,
        expense_name: formData.expense_name,
        amount: amount,
        currency: formData.currency,
        exchange_rate: exchangeRate,
        inr_equivalent: inrEquivalent,
        category: formData.category || null,
        expense_date: formData.expense_date,
        payment_method: formData.payment_method || null,
        notes: formData.notes || null,
      }

      let result
      if (editingExpense) {
        result = await supabase.from("monthly_expenses").update(expenseData).eq("id", editingExpense.id).select()
      } else {
        result = await supabase.from("monthly_expenses").insert([expenseData]).select()
      }

      if (result.error) {
        handleSupabaseError(result.error, editingExpense ? "update expense" : "add expense")
        return
      }

      setSuccess(editingExpense ? "Expense updated successfully!" : "Expense added successfully!")

      await fetchExpenses()
      setDialogOpen(false)
      resetForm()

      setTimeout(() => setSuccess(""), 3000)
    } catch (error: any) {
      console.error("Error saving expense:", error)
      setError(error.message || "Failed to save expense")
    }
  }

  const handleEdit = (expense: MonthlyExpense) => {
    setEditingExpense(expense)
    setFormData({
      expense_name: expense.expense_name,
      amount: expense.amount.toString(),
      currency: expense.currency,
      category: expense.category || "",
      expense_date: expense.expense_date,
      payment_method: expense.payment_method || "",
      notes: expense.notes || "",
      exchange_rate: expense.exchange_rate?.toString() || "",
    })
    setDialogOpen(true)
  }

  const handleDelete = async (id: string) => {
    try {
      setError("")
      const { error } = await supabase.from("monthly_expenses").delete().eq("id", id)

      if (error) {
        handleSupabaseError(error, "delete expense")
        return
      }

      setSuccess("Expense deleted successfully!")
      await fetchExpenses()
      setTimeout(() => setSuccess(""), 3000)
    } catch (error: any) {
      console.error("Error deleting expense:", error)
      setError(error.message || "Failed to delete expense")
    }
  }

  const resetForm = () => {
    setFormData({
      expense_name: "",
      amount: "",
      currency: "INR",
      category: "",
      expense_date: new Date().toISOString().split("T")[0],
      payment_method: "",
      notes: "",
      exchange_rate: currentExchangeRate.toString(),
    })
    setEditingExpense(null)
    setError("")
  }

  // Calculate totals in INR using stored INR equivalents
  const totalExpensesINR = expenses.reduce((sum, expense) => sum + (expense.inr_equivalent || 0), 0)
  const budgetUsedPercentage = (totalExpensesINR / monthlyBudget) * 100
  const remainingBudget = monthlyBudget - totalExpensesINR

  // Group expenses by category
  const expensesByCategory = expenses.reduce(
    (acc, expense) => {
      const category = expense.category || "Other"
      if (!acc[category]) {
        acc[category] = 0
      }
      acc[category] += expense.inr_equivalent || 0
      return acc
    },
    {} as Record<string, number>,
  )

  const topCategories = Object.entries(expensesByCategory)
    .sort(([, a], [, b]) => b - a)
    .slice(0, 5)

  const currentMonth = new Date().toLocaleString("default", { month: "long", year: "numeric" })

  if (loading) {
    return (
      <Card>
        <CardHeader>
          <div className="h-6 bg-muted rounded w-32 animate-pulse" />
          <div className="h-4 bg-muted rounded w-48 animate-pulse" />
        </CardHeader>
        <CardContent>
          <div className="space-y-4">
            {[...Array(3)].map((_, i) => (
              <div key={i} className="h-16 bg-muted rounded animate-pulse" />
            ))}
          </div>
        </CardContent>
      </Card>
    )
  }

  return (
    <div className="space-y-6">
      {/* Error and Success Messages */}
      {error && (
        <Alert variant="destructive">
          <AlertCircle className="h-4 w-4" />
          <AlertDescription>{error}</AlertDescription>
        </Alert>
      )}

      {success && (
        <Alert>
          <AlertDescription>{success}</AlertDescription>
        </Alert>
      )}

      {/* Currency Info Alert */}
      <Alert>
        <Info className="h-4 w-4" />
        <AlertDescription>
          <strong>Monthly Expense Tracking:</strong> Track your current month's expenses with accurate exchange rates.
          For USD expenses, enter the exchange rate used at the time of purchase. Current rate: ₹{currentExchangeRate}{" "}
          per $1.
        </AlertDescription>
      </Alert>

      {/* Budget Overview */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <TrendingUp className="h-5 w-5" />
            {currentMonth} Budget Overview
          </CardTitle>
          <CardDescription>Track your monthly spending against your budget</CardDescription>
        </CardHeader>
        <CardContent>
          <div className="space-y-4">
            <div className="flex items-center justify-between">
              <span className="text-sm font-medium">Budget Progress</span>
              <span className="text-sm text-muted-foreground">
                {formatINR(totalExpensesINR)} / {formatINR(monthlyBudget)}
              </span>
            </div>
            <Progress value={Math.min(budgetUsedPercentage, 100)} className="w-full" />
            <div className="grid grid-cols-3 gap-4 text-center">
              <div>
                <p className="text-2xl font-bold text-red-600">{formatINR(totalExpensesINR)}</p>
                <p className="text-xs text-muted-foreground">Spent</p>
              </div>
              <div>
                <p className={`text-2xl font-bold ${remainingBudget >= 0 ? "text-green-600" : "text-red-600"}`}>
                  {formatINR(Math.abs(remainingBudget))}
                </p>
                <p className="text-xs text-muted-foreground">{remainingBudget >= 0 ? "Remaining" : "Over Budget"}</p>
              </div>
              <div>
                <p className="text-2xl font-bold text-blue-600">{budgetUsedPercentage.toFixed(1)}%</p>
                <p className="text-xs text-muted-foreground">Used</p>
              </div>
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Summary Cards */}
      <div className="grid gap-4 md:grid-cols-4">
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Total Expenses</CardTitle>
            <IndianRupee className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold text-red-600">{formatINR(totalExpensesINR)}</div>
            <div className="text-sm text-muted-foreground">{expenses.length} transactions</div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Average per Day</CardTitle>
            <Calendar className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold text-orange-600">
              {formatINR(totalExpensesINR / new Date().getDate())}
            </div>
            <div className="text-sm text-muted-foreground">This month</div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Top Category</CardTitle>
            <ShoppingCart className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold text-purple-600">
              {topCategories[0] ? formatINR(topCategories[0][1]) : "₹0"}
            </div>
            <div className="text-sm text-muted-foreground">
              {topCategories[0] ? topCategories[0][0] : "No expenses"}
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Budget Status</CardTitle>
            <TrendingUp className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className={`text-2xl font-bold ${remainingBudget >= 0 ? "text-green-600" : "text-red-600"}`}>
              {remainingBudget >= 0 ? "On Track" : "Over Budget"}
            </div>
            <div className="text-sm text-muted-foreground">{budgetUsedPercentage.toFixed(1)}% used</div>
          </CardContent>
        </Card>
      </div>

      {/* Category Breakdown */}
      {topCategories.length > 0 && (
        <Card>
          <CardHeader>
            <CardTitle>Top Spending Categories</CardTitle>
            <CardDescription>Your highest expense categories this month</CardDescription>
          </CardHeader>
          <CardContent>
            <div className="space-y-4">
              {topCategories.map(([category, amount], index) => (
                <div key={category} className="flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <Badge variant="outline">{index + 1}</Badge>
                    <span className="font-medium">{category}</span>
                  </div>
                  <div className="text-right">
                    <div className="font-semibold">{formatINR(amount)}</div>
                    <div className="text-xs text-muted-foreground">
                      {((amount / totalExpensesINR) * 100).toFixed(1)}% of total
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </CardContent>
        </Card>
      )}

      {/* Expense Management */}
      <Card>
        <CardHeader>
          <div className="flex items-center justify-between">
            <div>
              <CardTitle className="flex items-center gap-2">
                <CreditCard className="h-5 w-5" />
                Monthly Expense Tracker
              </CardTitle>
              <CardDescription>Track your current month's expenses with accurate exchange rates</CardDescription>
            </div>
            <Dialog open={dialogOpen} onOpenChange={setDialogOpen}>
              <DialogTrigger asChild>
                <Button onClick={resetForm}>
                  <Plus className="mr-2 h-4 w-4" />
                  Add Expense
                </Button>
              </DialogTrigger>
              <DialogContent className="sm:max-w-[500px]">
                <DialogHeader>
                  <DialogTitle>{editingExpense ? "Edit Expense" : "Add New Expense"}</DialogTitle>
                  <DialogDescription>
                    {editingExpense
                      ? "Update your expense details"
                      : "Record a new expense with accurate exchange rate"}
                  </DialogDescription>
                </DialogHeader>
                <form onSubmit={handleSubmit} className="space-y-4">
                  <div className="space-y-2">
                    <Label htmlFor="expense_name">Expense Name *</Label>
                    <Input
                      id="expense_name"
                      placeholder="e.g., Grocery Shopping, Restaurant Bill"
                      value={formData.expense_name}
                      onChange={(e) => setFormData({ ...formData, expense_name: e.target.value })}
                      required
                    />
                  </div>

                  <div className="grid grid-cols-2 gap-4">
                    <div className="space-y-2">
                      <Label htmlFor="amount">Amount *</Label>
                      <Input
                        id="amount"
                        type="number"
                        step="0.01"
                        placeholder="0.00"
                        value={formData.amount}
                        onChange={(e) => setFormData({ ...formData, amount: e.target.value })}
                        required
                      />
                    </div>
                    <div className="space-y-2">
                      <Label htmlFor="currency">Currency *</Label>
                      <Select
                        value={formData.currency}
                        onValueChange={(value) => setFormData({ ...formData, currency: value })}
                      >
                        <SelectTrigger>
                          <SelectValue />
                        </SelectTrigger>
                        <SelectContent>
                          <SelectItem value="INR">INR (₹)</SelectItem>
                          <SelectItem value="USD">USD ($)</SelectItem>
                        </SelectContent>
                      </Select>
                    </div>
                  </div>

                  {formData.currency === "USD" && (
                    <div className="space-y-2">
                      <div className="flex items-center justify-between">
                        <Label htmlFor="exchange_rate">USD to INR Exchange Rate *</Label>
                        <Button
                          type="button"
                          variant="outline"
                          size="sm"
                          onClick={fetchCurrentExchangeRate}
                          disabled={fetchingRate}
                        >
                          <RefreshCw className={`mr-2 h-4 w-4 ${fetchingRate ? "animate-spin" : ""}`} />
                          Get Live Rate
                        </Button>
                      </div>
                      <Input
                        id="exchange_rate"
                        type="number"
                        step="0.000001"
                        placeholder="83.25"
                        value={formData.exchange_rate}
                        onChange={(e) => setFormData({ ...formData, exchange_rate: e.target.value })}
                        required
                      />
                      {formData.amount &&
                        formData.exchange_rate &&
                        Number.parseFloat(formData.amount) > 0 &&
                        Number.parseFloat(formData.exchange_rate) > 0 && (
                          <div className="text-sm text-muted-foreground bg-muted p-2 rounded">
                            <div className="flex items-center gap-1">
                              <IndianRupee className="h-3 w-3" />
                              <span>
                                INR Equivalent:{" "}
                                {formatINR(
                                  calculateINREquivalent(
                                    Number.parseFloat(formData.amount),
                                    Number.parseFloat(formData.exchange_rate),
                                  ),
                                )}
                              </span>
                            </div>
                            <div className="text-xs mt-1">
                              Rate: ₹{formData.exchange_rate} per $1 (Live: ₹{currentExchangeRate})
                            </div>
                          </div>
                        )}
                    </div>
                  )}

                  <div className="grid grid-cols-2 gap-4">
                    <div className="space-y-2">
                      <Label htmlFor="category">Category</Label>
                      <Select
                        value={formData.category}
                        onValueChange={(value) => setFormData({ ...formData, category: value })}
                      >
                        <SelectTrigger>
                          <SelectValue placeholder="Select category" />
                        </SelectTrigger>
                        <SelectContent>
                          {expenseCategories.map((category) => (
                            <SelectItem key={category} value={category}>
                              {category}
                            </SelectItem>
                          ))}
                        </SelectContent>
                      </Select>
                    </div>

                    <div className="space-y-2">
                      <Label htmlFor="payment_method">Payment Method</Label>
                      <Select
                        value={formData.payment_method}
                        onValueChange={(value) => setFormData({ ...formData, payment_method: value })}
                      >
                        <SelectTrigger>
                          <SelectValue placeholder="Select method" />
                        </SelectTrigger>
                        <SelectContent>
                          {paymentMethods.map((method) => (
                            <SelectItem key={method} value={method}>
                              {method}
                            </SelectItem>
                          ))}
                        </SelectContent>
                      </Select>
                    </div>
                  </div>

                  <div className="space-y-2">
                    <Label htmlFor="expense_date">Expense Date *</Label>
                    <Input
                      id="expense_date"
                      type="date"
                      value={formData.expense_date}
                      onChange={(e) => setFormData({ ...formData, expense_date: e.target.value })}
                      required
                    />
                  </div>

                  <div className="space-y-2">
                    <Label htmlFor="notes">Notes (Optional)</Label>
                    <Textarea
                      id="notes"
                      placeholder="Additional notes about this expense"
                      value={formData.notes}
                      onChange={(e) => setFormData({ ...formData, notes: e.target.value })}
                    />
                  </div>

                  <div className="flex justify-end space-x-2">
                    <Button type="button" variant="outline" onClick={() => setDialogOpen(false)}>
                      Cancel
                    </Button>
                    <Button type="submit">{editingExpense ? "Update" : "Add"} Expense</Button>
                  </div>
                </form>
              </DialogContent>
            </Dialog>
          </div>
        </CardHeader>
        <CardContent>
          {expenses.length === 0 ? (
            <div className="text-center py-12">
              <CreditCard className="mx-auto h-12 w-12 text-muted-foreground mb-4" />
              <h3 className="text-lg font-semibold text-gray-900 mb-2">No expenses recorded</h3>
              <p className="text-sm text-gray-500 mb-4">Start tracking your monthly expenses.</p>
              <Button onClick={() => setDialogOpen(true)}>
                <Plus className="mr-2 h-4 w-4" />
                Add Your First Expense
              </Button>
            </div>
          ) : (
            <div className="overflow-x-auto">
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Expense</TableHead>
                    <TableHead>Category</TableHead>
                    <TableHead>Original Amount</TableHead>
                    <TableHead>INR Equivalent</TableHead>
                    <TableHead>Exchange Rate</TableHead>
                    <TableHead>Date</TableHead>
                    <TableHead>Payment Method</TableHead>
                    <TableHead className="text-right">Actions</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {expenses.map((expense) => (
                    <TableRow key={expense.id}>
                      <TableCell className="font-medium">{expense.expense_name}</TableCell>
                      <TableCell>{expense.category && <Badge variant="secondary">{expense.category}</Badge>}</TableCell>
                      <TableCell className="font-semibold">
                        {expense.currency === "USD" ? formatUSD(expense.amount) : formatINR(expense.amount)}
                      </TableCell>
                      <TableCell className="text-red-600 font-semibold">
                        {formatINR(expense.inr_equivalent || 0)}
                      </TableCell>
                      <TableCell>
                        {expense.currency === "USD" && expense.exchange_rate ? (
                          <Badge variant="outline">₹{expense.exchange_rate}</Badge>
                        ) : (
                          <span className="text-muted-foreground">—</span>
                        )}
                      </TableCell>
                      <TableCell>{new Date(expense.expense_date).toLocaleDateString()}</TableCell>
                      <TableCell>
                        {expense.payment_method && <Badge variant="outline">{expense.payment_method}</Badge>}
                      </TableCell>
                      <TableCell className="text-right">
                        <div className="flex justify-end space-x-2">
                          <Button variant="ghost" size="sm" onClick={() => handleEdit(expense)}>
                            <Edit className="h-4 w-4" />
                          </Button>
                          <Button variant="ghost" size="sm" onClick={() => handleDelete(expense.id)}>
                            <Trash2 className="h-4 w-4" />
                          </Button>
                        </div>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  )
}
