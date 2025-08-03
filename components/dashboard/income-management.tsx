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
import { Switch } from "@/components/ui/switch"
import { Badge } from "@/components/ui/badge"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { Alert, AlertDescription } from "@/components/ui/alert"
import { Plus, Edit, Trash2, DollarSign, Calendar, Repeat, AlertCircle, IndianRupee, Info } from "lucide-react"
import { formatUSD, formatINR, convertUSDToINR, EXCHANGE_RATES } from "@/lib/utils"

interface Income {
  id: string
  source: string
  amount: number
  currency: string
  category: string | null
  description: string | null
  date: string
  is_recurring: boolean
  recurring_frequency: string | null
}

const incomeCategories = ["Salary", "Freelance", "Business", "Investment", "Rental", "Bonus", "Commission", "Other"]

const frequencies = [
  { value: "weekly", label: "Weekly" },
  { value: "monthly", label: "Monthly" },
  { value: "quarterly", label: "Quarterly" },
  { value: "yearly", label: "Yearly" },
]

export default function IncomeManagement() {
  const [incomes, setIncomes] = useState<Income[]>([])
  const [loading, setLoading] = useState(true)
  const [dialogOpen, setDialogOpen] = useState(false)
  const [editingIncome, setEditingIncome] = useState<Income | null>(null)
  const [error, setError] = useState("")
  const [success, setSuccess] = useState("")

  const [formData, setFormData] = useState({
    source: "",
    amount: "",
    category: "",
    description: "",
    date: new Date().toISOString().split("T")[0],
    is_recurring: false,
    recurring_frequency: "",
  })

  useEffect(() => {
    fetchIncomes()
  }, [])

  const fetchIncomes = async () => {
    try {
      setError("")
      const {
        data: { user },
      } = await supabase.auth.getUser()

      if (!user) {
        setError("Please log in to view income")
        return
      }

      console.log("Fetching incomes for user:", user.id)

      const { data, error } = await supabase
        .from("income")
        .select("*")
        .eq("user_id", user.id)
        .order("date", { ascending: false })

      if (error) {
        handleSupabaseError(error, "fetch income")
        return
      }

      console.log("Fetched incomes:", data)
      setIncomes(data || [])
    } catch (error: any) {
      console.error("Error fetching incomes:", error)
      setError(error.message || "Failed to fetch income data")
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
        setError("Please log in to add income")
        return
      }

      console.log("Submitting income data...")

      // Always store income in USD
      const incomeData = {
        user_id: user.id,
        source: formData.source,
        amount: Number.parseFloat(formData.amount), // This is USD amount
        currency: "USD", // Always USD for income
        category: formData.category || null,
        description: formData.description || null,
        date: formData.date,
        is_recurring: formData.is_recurring,
        recurring_frequency: formData.is_recurring ? formData.recurring_frequency : null,
      }

      console.log("Income data to submit:", incomeData)

      let result
      if (editingIncome) {
        result = await supabase.from("income").update(incomeData).eq("id", editingIncome.id).select()
      } else {
        result = await supabase.from("income").insert([incomeData]).select()
      }

      if (result.error) {
        handleSupabaseError(result.error, editingIncome ? "update income" : "add income")
        return
      }

      console.log("Income saved successfully:", result.data)
      setSuccess(editingIncome ? "Income updated successfully!" : "Income added successfully!")

      await fetchIncomes()
      setDialogOpen(false)
      resetForm()

      setTimeout(() => setSuccess(""), 3000)
    } catch (error: any) {
      console.error("Error saving income:", error)
      setError(error.message || "Failed to save income")
    }
  }

  const handleEdit = (income: Income) => {
    setEditingIncome(income)
    setFormData({
      source: income.source,
      amount: income.amount.toString(), // This is the USD amount
      category: income.category || "",
      description: income.description || "",
      date: income.date,
      is_recurring: income.is_recurring,
      recurring_frequency: income.recurring_frequency || "",
    })
    setDialogOpen(true)
  }

  const handleDelete = async (id: string) => {
    try {
      setError("")
      const { error } = await supabase.from("income").delete().eq("id", id)

      if (error) {
        handleSupabaseError(error, "delete income")
        return
      }

      setSuccess("Income deleted successfully!")
      await fetchIncomes()
      setTimeout(() => setSuccess(""), 3000)
    } catch (error: any) {
      console.error("Error deleting income:", error)
      setError(error.message || "Failed to delete income")
    }
  }

  const resetForm = () => {
    setFormData({
      source: "",
      amount: "",
      category: "",
      description: "",
      date: new Date().toISOString().split("T")[0],
      is_recurring: false,
      recurring_frequency: "",
    })
    setEditingIncome(null)
    setError("")
  }

  // Calculate totals in USD (all income is stored in USD)
  const totalIncomeUSD = incomes.reduce((sum, income) => sum + income.amount, 0)
  const monthlyRecurringUSD = incomes
    .filter((income) => income.is_recurring && income.recurring_frequency === "monthly")
    .reduce((sum, income) => sum + income.amount, 0)

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
          <strong>Currency Info:</strong> All income is tracked in USD ($) and automatically converted to INR (₹) using
          the current exchange rate of ₹{EXCHANGE_RATES.USD_TO_INR} per $1.
        </AlertDescription>
      </Alert>

      {/* Summary Cards */}
      <div className="grid gap-4 md:grid-cols-3">
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Total Income</CardTitle>
            <div className="flex items-center gap-1">
              <DollarSign className="h-4 w-4 text-muted-foreground" />
              <IndianRupee className="h-3 w-3 text-muted-foreground" />
            </div>
          </CardHeader>
          <CardContent>
            <div className="space-y-1">
              <div className="text-2xl font-bold text-green-600">{formatUSD(totalIncomeUSD)}</div>
              <div className="text-sm text-muted-foreground">≈ {formatINR(convertUSDToINR(totalIncomeUSD))}</div>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Monthly Recurring</CardTitle>
            <Repeat className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="space-y-1">
              <div className="text-2xl font-bold text-blue-600">{formatUSD(monthlyRecurringUSD)}</div>
              <div className="text-sm text-muted-foreground">≈ {formatINR(convertUSDToINR(monthlyRecurringUSD))}</div>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Income Sources</CardTitle>
            <Calendar className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{incomes.length}</div>
            <div className="text-sm text-muted-foreground">Active sources</div>
          </CardContent>
        </Card>
      </div>

      {/* Income Management */}
      <Card>
        <CardHeader>
          <div className="flex items-center justify-between">
            <div>
              <CardTitle className="flex items-center gap-2">
                <DollarSign className="h-5 w-5" />
                Income Management
              </CardTitle>
              <CardDescription>Track and manage your income sources in USD with INR conversion</CardDescription>
            </div>
            <Dialog open={dialogOpen} onOpenChange={setDialogOpen}>
              <DialogTrigger asChild>
                <Button onClick={resetForm}>
                  <Plus className="mr-2 h-4 w-4" />
                  Add Income
                </Button>
              </DialogTrigger>
              <DialogContent className="sm:max-w-[500px]">
                <DialogHeader>
                  <DialogTitle>{editingIncome ? "Edit Income" : "Add New Income"}</DialogTitle>
                  <DialogDescription>
                    {editingIncome ? "Update your income details" : "Add a new income source in USD"}
                  </DialogDescription>
                </DialogHeader>
                <form onSubmit={handleSubmit} className="space-y-4">
                  <div className="space-y-2">
                    <Label htmlFor="source">Income Source *</Label>
                    <Input
                      id="source"
                      placeholder="e.g., Salary, Freelance Project, Business Income"
                      value={formData.source}
                      onChange={(e) => setFormData({ ...formData, source: e.target.value })}
                      required
                    />
                  </div>

                  <div className="space-y-2">
                    <Label htmlFor="amount">Amount in USD ($) *</Label>
                    <div className="relative">
                      <DollarSign className="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-muted-foreground" />
                      <Input
                        id="amount"
                        type="number"
                        step="0.01"
                        placeholder="0.00"
                        className="pl-10"
                        value={formData.amount}
                        onChange={(e) => setFormData({ ...formData, amount: e.target.value })}
                        required
                      />
                    </div>
                    {formData.amount && Number.parseFloat(formData.amount) > 0 && (
                      <div className="text-sm text-muted-foreground bg-muted p-2 rounded">
                        <div className="flex items-center gap-1">
                          <IndianRupee className="h-3 w-3" />
                          <span>Equivalent: {formatINR(convertUSDToINR(Number.parseFloat(formData.amount)))}</span>
                        </div>
                        <div className="text-xs mt-1">Exchange rate: ₹{EXCHANGE_RATES.USD_TO_INR} per $1</div>
                      </div>
                    )}
                  </div>

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
                        {incomeCategories.map((category) => (
                          <SelectItem key={category} value={category}>
                            {category}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  </div>

                  <div className="space-y-2">
                    <Label htmlFor="date">Date *</Label>
                    <Input
                      id="date"
                      type="date"
                      value={formData.date}
                      onChange={(e) => setFormData({ ...formData, date: e.target.value })}
                      required
                    />
                  </div>

                  <div className="flex items-center space-x-2">
                    <Switch
                      id="recurring"
                      checked={formData.is_recurring}
                      onCheckedChange={(checked) => setFormData({ ...formData, is_recurring: checked })}
                    />
                    <Label htmlFor="recurring">Recurring Income</Label>
                  </div>

                  {formData.is_recurring && (
                    <div className="space-y-2">
                      <Label htmlFor="frequency">Frequency</Label>
                      <Select
                        value={formData.recurring_frequency}
                        onValueChange={(value) => setFormData({ ...formData, recurring_frequency: value })}
                      >
                        <SelectTrigger>
                          <SelectValue placeholder="Select frequency" />
                        </SelectTrigger>
                        <SelectContent>
                          {frequencies.map((freq) => (
                            <SelectItem key={freq.value} value={freq.value}>
                              {freq.label}
                            </SelectItem>
                          ))}
                        </SelectContent>
                      </Select>
                    </div>
                  )}

                  <div className="space-y-2">
                    <Label htmlFor="description">Description (Optional)</Label>
                    <Textarea
                      id="description"
                      placeholder="Additional notes about this income"
                      value={formData.description}
                      onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                    />
                  </div>

                  <div className="flex justify-end space-x-2">
                    <Button type="button" variant="outline" onClick={() => setDialogOpen(false)}>
                      Cancel
                    </Button>
                    <Button type="submit">{editingIncome ? "Update" : "Add"} Income</Button>
                  </div>
                </form>
              </DialogContent>
            </Dialog>
          </div>
        </CardHeader>
        <CardContent>
          {incomes.length === 0 ? (
            <div className="text-center py-12">
              <DollarSign className="mx-auto h-12 w-12 text-muted-foreground mb-4" />
              <h3 className="text-lg font-semibold text-gray-900 mb-2">No income recorded</h3>
              <p className="text-sm text-gray-500 mb-4">Get started by adding your first income source in USD.</p>
              <Button onClick={() => setDialogOpen(true)}>
                <Plus className="mr-2 h-4 w-4" />
                Add Your First Income
              </Button>
            </div>
          ) : (
            <div className="overflow-x-auto">
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Source</TableHead>
                    <TableHead>Category</TableHead>
                    <TableHead>Amount (USD)</TableHead>
                    <TableHead>Amount (INR)</TableHead>
                    <TableHead>Date</TableHead>
                    <TableHead>Type</TableHead>
                    <TableHead className="text-right">Actions</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {incomes.map((income) => (
                    <TableRow key={income.id}>
                      <TableCell className="font-medium">{income.source}</TableCell>
                      <TableCell>{income.category && <Badge variant="secondary">{income.category}</Badge>}</TableCell>
                      <TableCell className="text-green-600 font-semibold">{formatUSD(income.amount)}</TableCell>
                      <TableCell className="text-blue-600 font-semibold">
                        {formatINR(convertUSDToINR(income.amount))}
                      </TableCell>
                      <TableCell>{new Date(income.date).toLocaleDateString()}</TableCell>
                      <TableCell>
                        {income.is_recurring ? (
                          <Badge variant="default">
                            <Repeat className="mr-1 h-3 w-3" />
                            {income.recurring_frequency}
                          </Badge>
                        ) : (
                          <Badge variant="outline">One-time</Badge>
                        )}
                      </TableCell>
                      <TableCell className="text-right">
                        <div className="flex justify-end space-x-2">
                          <Button variant="ghost" size="sm" onClick={() => handleEdit(income)}>
                            <Edit className="h-4 w-4" />
                          </Button>
                          <Button variant="ghost" size="sm" onClick={() => handleDelete(income.id)}>
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
