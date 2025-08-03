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
import { Progress } from "@/components/ui/progress"
import { Alert, AlertDescription } from "@/components/ui/alert"
import { Tabs, TabsList, TabsTrigger } from "@/components/ui/tabs"
import {
  Plus,
  Edit,
  Trash2,
  CreditCard,
  TrendingDown,
  Calendar,
  DollarSign,
  AlertCircle,
  IndianRupee,
} from "lucide-react"
import { formatINR, formatUSD, convertCurrency } from "@/lib/utils"

interface Loan {
  id: string
  loan_name: string
  principal_amount: number
  current_balance: number
  interest_rate: number | null
  monthly_payment: number | null
  start_date: string
  end_date: string | null
  lender: string | null
  loan_type: string | null
  status: string | null
  currency: string | null
}

interface LoanPayment {
  id: string
  loan_id: string
  amount: number
  principal_amount: number | null
  interest_amount: number | null
  payment_date: string
  payment_method: string | null
  notes: string | null
  currency: string | null
}

const loanTypes = [
  "Personal",
  "Home Loan",
  "Car Loan",
  "Education Loan",
  "Credit Card",
  "Business Loan",
  "Gold Loan",
  "Other",
]

const paymentMethods = ["UPI", "Net Banking", "Bank Transfer", "Check", "Cash", "Credit Card", "Debit Card", "Other"]

const currencies = [
  { value: "INR", label: "Indian Rupee (₹)", symbol: "₹" },
  { value: "USD", label: "US Dollar ($)", symbol: "$" },
]

export default function LoanTracker() {
  const [loans, setLoans] = useState<Loan[]>([])
  const [payments, setPayments] = useState<LoanPayment[]>([])
  const [loading, setLoading] = useState(true)
  const [loanDialogOpen, setLoanDialogOpen] = useState(false)
  const [paymentDialogOpen, setPaymentDialogOpen] = useState(false)
  const [editingLoan, setEditingLoan] = useState<Loan | null>(null)
  const [error, setError] = useState("")
  const [success, setSuccess] = useState("")
  const [displayCurrency, setDisplayCurrency] = useState<"INR" | "USD">("INR")

  const [loanFormData, setLoanFormData] = useState({
    loan_name: "",
    principal_amount: "",
    current_balance: "",
    interest_rate: "",
    monthly_payment: "",
    start_date: new Date().toISOString().split("T")[0],
    end_date: "",
    lender: "",
    loan_type: "",
    status: "active",
    currency: "INR",
  })

  const [paymentFormData, setPaymentFormData] = useState({
    loan_id: "",
    amount: "",
    principal_amount: "",
    interest_amount: "",
    payment_date: new Date().toISOString().split("T")[0],
    payment_method: "",
    notes: "",
    currency: "INR",
  })

  useEffect(() => {
    fetchLoans()
    fetchPayments()
  }, [])

  const fetchLoans = async () => {
    try {
      setError("")
      const {
        data: { user },
      } = await supabase.auth.getUser()

      if (!user) {
        setError("Please log in to view loans")
        return
      }

      console.log("Fetching loans for user:", user.id)

      const { data, error } = await supabase
        .from("loans")
        .select("*")
        .eq("user_id", user.id)
        .order("created_at", { ascending: false })

      if (error) {
        handleSupabaseError(error, "fetch loans")
        return
      }

      console.log("Fetched loans:", data)
      setLoans(data || [])
    } catch (error: any) {
      console.error("Error fetching loans:", error)
      setError(error.message || "Failed to fetch loans")
    } finally {
      setLoading(false)
    }
  }

  const fetchPayments = async () => {
    try {
      const {
        data: { user },
      } = await supabase.auth.getUser()
      if (!user) return

      const { data, error } = await supabase
        .from("loan_payments")
        .select("*")
        .eq("user_id", user.id)
        .order("payment_date", { ascending: false })

      if (error) {
        console.error("Error fetching payments:", error)
        return
      }

      setPayments(data || [])
    } catch (error) {
      console.error("Error fetching payments:", error)
    }
  }

  const handleLoanSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError("")
    setSuccess("")

    try {
      const {
        data: { user },
      } = await supabase.auth.getUser()

      if (!user) {
        setError("Please log in to add loans")
        return
      }

      console.log("Submitting loan data...")

      const loanData = {
        user_id: user.id,
        loan_name: loanFormData.loan_name,
        principal_amount: Number.parseFloat(loanFormData.principal_amount),
        current_balance: Number.parseFloat(loanFormData.current_balance),
        interest_rate: loanFormData.interest_rate ? Number.parseFloat(loanFormData.interest_rate) : null,
        monthly_payment: loanFormData.monthly_payment ? Number.parseFloat(loanFormData.monthly_payment) : null,
        start_date: loanFormData.start_date,
        end_date: loanFormData.end_date || null,
        lender: loanFormData.lender || null,
        loan_type: loanFormData.loan_type || null,
        status: loanFormData.status,
        currency: loanFormData.currency,
      }

      console.log("Loan data to submit:", loanData)

      let result
      if (editingLoan) {
        result = await supabase.from("loans").update(loanData).eq("id", editingLoan.id).select()
      } else {
        result = await supabase.from("loans").insert([loanData]).select()
      }

      if (result.error) {
        handleSupabaseError(result.error, editingLoan ? "update loan" : "add loan")
        return
      }

      console.log("Loan saved successfully:", result.data)
      setSuccess(editingLoan ? "Loan updated successfully!" : "Loan added successfully!")

      await fetchLoans()
      setLoanDialogOpen(false)
      resetLoanForm()

      setTimeout(() => setSuccess(""), 3000)
    } catch (error: any) {
      console.error("Error saving loan:", error)
      setError(error.message || "Failed to save loan")
    }
  }

  const handlePaymentSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError("")
    setSuccess("")

    try {
      const {
        data: { user },
      } = await supabase.auth.getUser()

      if (!user) {
        setError("Please log in to record payments")
        return
      }

      const selectedLoan = loans.find((l) => l.id === paymentFormData.loan_id)
      if (!selectedLoan) {
        setError("Please select a valid loan")
        return
      }

      // Convert payment amount to loan currency if different
      let paymentAmount = Number.parseFloat(paymentFormData.amount)
      if (paymentFormData.currency !== selectedLoan.currency) {
        paymentAmount = convertCurrency(paymentAmount, paymentFormData.currency, selectedLoan.currency || "INR")
      }

      const paymentData = {
        loan_id: paymentFormData.loan_id,
        user_id: user.id,
        amount: paymentAmount,
        principal_amount: paymentFormData.principal_amount ? Number.parseFloat(paymentFormData.principal_amount) : null,
        interest_amount: paymentFormData.interest_amount ? Number.parseFloat(paymentFormData.interest_amount) : null,
        payment_date: paymentFormData.payment_date,
        payment_method: paymentFormData.payment_method || null,
        notes: paymentFormData.notes || null,
        currency: selectedLoan.currency || "INR",
      }

      const { error } = await supabase.from("loan_payments").insert([paymentData])

      if (error) {
        handleSupabaseError(error, "record payment")
        return
      }

      // Update loan balance
      const newBalance = selectedLoan.current_balance - paymentAmount
      await supabase
        .from("loans")
        .update({
          current_balance: Math.max(0, newBalance),
          status: newBalance <= 0 ? "paid_off" : "active",
        })
        .eq("id", paymentFormData.loan_id)

      setSuccess("Payment recorded successfully!")
      await fetchLoans()
      await fetchPayments()
      setPaymentDialogOpen(false)
      resetPaymentForm()

      setTimeout(() => setSuccess(""), 3000)
    } catch (error: any) {
      console.error("Error saving payment:", error)
      setError(error.message || "Failed to record payment")
    }
  }

  const handleEditLoan = (loan: Loan) => {
    setEditingLoan(loan)
    setLoanFormData({
      loan_name: loan.loan_name,
      principal_amount: loan.principal_amount.toString(),
      current_balance: loan.current_balance.toString(),
      interest_rate: loan.interest_rate?.toString() || "",
      monthly_payment: loan.monthly_payment?.toString() || "",
      start_date: loan.start_date,
      end_date: loan.end_date || "",
      lender: loan.lender || "",
      loan_type: loan.loan_type || "",
      status: loan.status || "active",
      currency: loan.currency || "INR",
    })
    setLoanDialogOpen(true)
  }

  const handleDeleteLoan = async (id: string) => {
    try {
      setError("")
      const { error } = await supabase.from("loans").delete().eq("id", id)

      if (error) {
        handleSupabaseError(error, "delete loan")
        return
      }

      setSuccess("Loan deleted successfully!")
      await fetchLoans()
      setTimeout(() => setSuccess(""), 3000)
    } catch (error: any) {
      console.error("Error deleting loan:", error)
      setError(error.message || "Failed to delete loan")
    }
  }

  const resetLoanForm = () => {
    setLoanFormData({
      loan_name: "",
      principal_amount: "",
      current_balance: "",
      interest_rate: "",
      monthly_payment: "",
      start_date: new Date().toISOString().split("T")[0],
      end_date: "",
      lender: "",
      loan_type: "",
      status: "active",
      currency: "INR",
    })
    setEditingLoan(null)
    setError("")
  }

  const resetPaymentForm = () => {
    setPaymentFormData({
      loan_id: "",
      amount: "",
      principal_amount: "",
      interest_amount: "",
      payment_date: new Date().toISOString().split("T")[0],
      payment_method: "",
      notes: "",
      currency: "INR",
    })
    setError("")
  }

  // Calculate totals in display currency
  const calculateTotals = () => {
    let totalBalance = 0
    let totalPrincipal = 0

    loans.forEach((loan) => {
      const loanCurrency = loan.currency || "INR"
      const balanceInDisplayCurrency =
        displayCurrency === loanCurrency
          ? loan.current_balance
          : convertCurrency(loan.current_balance, loanCurrency, displayCurrency)
      const principalInDisplayCurrency =
        displayCurrency === loanCurrency
          ? loan.principal_amount
          : convertCurrency(loan.principal_amount, loanCurrency, displayCurrency)

      totalBalance += balanceInDisplayCurrency
      totalPrincipal += principalInDisplayCurrency
    })

    return {
      totalBalance,
      totalPrincipal,
      totalPaid: totalPrincipal - totalBalance,
    }
  }

  const { totalBalance, totalPrincipal, totalPaid } = calculateTotals()

  const formatDisplayCurrency = (amount: number) => {
    return displayCurrency === "INR" ? formatINR(amount) : formatUSD(amount)
  }

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

      {/* Currency Toggle */}
      <div className="flex justify-between items-center">
        <div>
          <h2 className="text-2xl font-bold">Loan Management</h2>
          <p className="text-muted-foreground">Track your loans and repayment progress</p>
        </div>
        <div className="flex items-center space-x-2">
          <Label htmlFor="currency-toggle" className="text-sm">
            Display Currency:
          </Label>
          <Tabs value={displayCurrency} onValueChange={(value) => setDisplayCurrency(value as "INR" | "USD")}>
            <TabsList className="grid w-full grid-cols-2">
              <TabsTrigger value="INR" className="flex items-center gap-1">
                <IndianRupee className="h-3 w-3" />
                INR
              </TabsTrigger>
              <TabsTrigger value="USD" className="flex items-center gap-1">
                <DollarSign className="h-3 w-3" />
                USD
              </TabsTrigger>
            </TabsList>
          </Tabs>
        </div>
      </div>

      {/* Summary Cards */}
      <div className="grid gap-4 md:grid-cols-4">
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Total Debt</CardTitle>
            <CreditCard className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold text-red-600">{formatDisplayCurrency(totalBalance)}</div>
            <p className="text-xs text-muted-foreground mt-1">
              {displayCurrency === "INR" ? "Indian Rupees" : "US Dollars"}
            </p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Total Paid</CardTitle>
            <TrendingDown className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold text-green-600">{formatDisplayCurrency(totalPaid)}</div>
            <p className="text-xs text-muted-foreground mt-1">
              {displayCurrency === "INR" ? "Indian Rupees" : "US Dollars"}
            </p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Active Loans</CardTitle>
            <Calendar className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{loans.filter((loan) => loan.status === "active").length}</div>
            <p className="text-xs text-muted-foreground mt-1">Currently tracking</p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Progress</CardTitle>
            <DollarSign className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">
              {totalPrincipal > 0 ? Math.round((totalPaid / totalPrincipal) * 100) : 0}%
            </div>
            <p className="text-xs text-muted-foreground mt-1">Overall repayment</p>
          </CardContent>
        </Card>
      </div>

      {/* Loan Management */}
      <Card>
        <CardHeader>
          <div className="flex items-center justify-between">
            <div>
              <CardTitle className="flex items-center gap-2">
                <IndianRupee className="h-5 w-5" />
                Your Loans
              </CardTitle>
              <CardDescription>Manage your loans in INR and USD</CardDescription>
            </div>
            <div className="flex space-x-2">
              <Dialog open={loanDialogOpen} onOpenChange={setLoanDialogOpen}>
                <DialogTrigger asChild>
                  <Button onClick={resetLoanForm}>
                    <Plus className="mr-2 h-4 w-4" />
                    Add Loan
                  </Button>
                </DialogTrigger>
                <DialogContent className="sm:max-w-[500px] max-h-[90vh] overflow-y-auto">
                  <DialogHeader>
                    <DialogTitle>{editingLoan ? "Edit Loan" : "Add New Loan"}</DialogTitle>
                    <DialogDescription>
                      {editingLoan ? "Update your loan details" : "Add a new loan to track your debt"}
                    </DialogDescription>
                  </DialogHeader>
                  <form onSubmit={handleLoanSubmit} className="space-y-4">
                    <div className="space-y-2">
                      <Label htmlFor="loan_name">Loan Name *</Label>
                      <Input
                        id="loan_name"
                        placeholder="e.g., Home Loan, Car Loan, Personal Loan"
                        value={loanFormData.loan_name}
                        onChange={(e) => setLoanFormData({ ...loanFormData, loan_name: e.target.value })}
                        required
                      />
                    </div>

                    <div className="space-y-2">
                      <Label htmlFor="currency">Currency *</Label>
                      <Select
                        value={loanFormData.currency}
                        onValueChange={(value) => setLoanFormData({ ...loanFormData, currency: value })}
                      >
                        <SelectTrigger>
                          <SelectValue placeholder="Select currency" />
                        </SelectTrigger>
                        <SelectContent>
                          {currencies.map((currency) => (
                            <SelectItem key={currency.value} value={currency.value}>
                              {currency.label}
                            </SelectItem>
                          ))}
                        </SelectContent>
                      </Select>
                    </div>

                    <div className="grid grid-cols-2 gap-4">
                      <div className="space-y-2">
                        <Label htmlFor="principal_amount">
                          Principal Amount * ({loanFormData.currency === "INR" ? "₹" : "$"})
                        </Label>
                        <Input
                          id="principal_amount"
                          type="number"
                          step="0.01"
                          placeholder="0.00"
                          value={loanFormData.principal_amount}
                          onChange={(e) => setLoanFormData({ ...loanFormData, principal_amount: e.target.value })}
                          required
                        />
                      </div>
                      <div className="space-y-2">
                        <Label htmlFor="current_balance">
                          Current Balance * ({loanFormData.currency === "INR" ? "₹" : "$"})
                        </Label>
                        <Input
                          id="current_balance"
                          type="number"
                          step="0.01"
                          placeholder="0.00"
                          value={loanFormData.current_balance}
                          onChange={(e) => setLoanFormData({ ...loanFormData, current_balance: e.target.value })}
                          required
                        />
                      </div>
                    </div>

                    <div className="grid grid-cols-2 gap-4">
                      <div className="space-y-2">
                        <Label htmlFor="interest_rate">Interest Rate (% per annum)</Label>
                        <Input
                          id="interest_rate"
                          type="number"
                          step="0.01"
                          placeholder="e.g., 8.5"
                          value={loanFormData.interest_rate}
                          onChange={(e) => setLoanFormData({ ...loanFormData, interest_rate: e.target.value })}
                        />
                      </div>
                      <div className="space-y-2">
                        <Label htmlFor="monthly_payment">
                          Monthly EMI ({loanFormData.currency === "INR" ? "₹" : "$"})
                        </Label>
                        <Input
                          id="monthly_payment"
                          type="number"
                          step="0.01"
                          placeholder="0.00"
                          value={loanFormData.monthly_payment}
                          onChange={(e) => setLoanFormData({ ...loanFormData, monthly_payment: e.target.value })}
                        />
                      </div>
                    </div>

                    <div className="space-y-2">
                      <Label htmlFor="loan_type">Loan Type</Label>
                      <Select
                        value={loanFormData.loan_type}
                        onValueChange={(value) => setLoanFormData({ ...loanFormData, loan_type: value })}
                      >
                        <SelectTrigger>
                          <SelectValue placeholder="Select loan type" />
                        </SelectTrigger>
                        <SelectContent>
                          {loanTypes.map((type) => (
                            <SelectItem key={type} value={type.toLowerCase().replace(" ", "_")}>
                              {type}
                            </SelectItem>
                          ))}
                        </SelectContent>
                      </Select>
                    </div>

                    <div className="space-y-2">
                      <Label htmlFor="lender">Lender/Bank</Label>
                      <Input
                        id="lender"
                        placeholder="e.g., SBI, HDFC Bank, ICICI Bank, Axis Bank"
                        value={loanFormData.lender}
                        onChange={(e) => setLoanFormData({ ...loanFormData, lender: e.target.value })}
                      />
                    </div>

                    <div className="grid grid-cols-2 gap-4">
                      <div className="space-y-2">
                        <Label htmlFor="start_date">Start Date *</Label>
                        <Input
                          id="start_date"
                          type="date"
                          value={loanFormData.start_date}
                          onChange={(e) => setLoanFormData({ ...loanFormData, start_date: e.target.value })}
                          required
                        />
                      </div>
                      <div className="space-y-2">
                        <Label htmlFor="end_date">End Date (Optional)</Label>
                        <Input
                          id="end_date"
                          type="date"
                          value={loanFormData.end_date}
                          onChange={(e) => setLoanFormData({ ...loanFormData, end_date: e.target.value })}
                        />
                      </div>
                    </div>

                    <div className="flex justify-end space-x-2">
                      <Button type="button" variant="outline" onClick={() => setLoanDialogOpen(false)}>
                        Cancel
                      </Button>
                      <Button type="submit">{editingLoan ? "Update" : "Add"} Loan</Button>
                    </div>
                  </form>
                </DialogContent>
              </Dialog>

              <Dialog open={paymentDialogOpen} onOpenChange={setPaymentDialogOpen}>
                <DialogTrigger asChild>
                  <Button variant="outline" onClick={resetPaymentForm}>
                    <IndianRupee className="mr-2 h-4 w-4" />
                    Record Payment
                  </Button>
                </DialogTrigger>
                <DialogContent className="sm:max-w-[425px]">
                  <DialogHeader>
                    <DialogTitle>Record Loan Payment</DialogTitle>
                    <DialogDescription>Record a payment made towards one of your loans</DialogDescription>
                  </DialogHeader>
                  <form onSubmit={handlePaymentSubmit} className="space-y-4">
                    <div className="space-y-2">
                      <Label htmlFor="payment_loan_id">Select Loan *</Label>
                      <Select
                        value={paymentFormData.loan_id}
                        onValueChange={(value) => {
                          const selectedLoan = loans.find((l) => l.id === value)
                          setPaymentFormData({
                            ...paymentFormData,
                            loan_id: value,
                            currency: selectedLoan?.currency || "INR",
                          })
                        }}
                      >
                        <SelectTrigger>
                          <SelectValue placeholder="Select a loan" />
                        </SelectTrigger>
                        <SelectContent>
                          {loans
                            .filter((loan) => loan.status === "active")
                            .map((loan) => (
                              <SelectItem key={loan.id} value={loan.id}>
                                {loan.loan_name} -{" "}
                                {loan.currency === "INR"
                                  ? formatINR(loan.current_balance)
                                  : formatUSD(loan.current_balance)}{" "}
                                remaining
                              </SelectItem>
                            ))}
                        </SelectContent>
                      </Select>
                    </div>

                    <div className="space-y-2">
                      <Label htmlFor="payment_currency">Payment Currency</Label>
                      <Select
                        value={paymentFormData.currency}
                        onValueChange={(value) => setPaymentFormData({ ...paymentFormData, currency: value })}
                      >
                        <SelectTrigger>
                          <SelectValue placeholder="Select currency" />
                        </SelectTrigger>
                        <SelectContent>
                          {currencies.map((currency) => (
                            <SelectItem key={currency.value} value={currency.value}>
                              {currency.label}
                            </SelectItem>
                          ))}
                        </SelectContent>
                      </Select>
                    </div>

                    <div className="space-y-2">
                      <Label htmlFor="payment_amount">
                        Payment Amount * ({paymentFormData.currency === "INR" ? "₹" : "$"})
                      </Label>
                      <Input
                        id="payment_amount"
                        type="number"
                        step="0.01"
                        placeholder="0.00"
                        value={paymentFormData.amount}
                        onChange={(e) => setPaymentFormData({ ...paymentFormData, amount: e.target.value })}
                        required
                      />
                    </div>

                    <div className="grid grid-cols-2 gap-4">
                      <div className="space-y-2">
                        <Label htmlFor="principal_payment">Principal Amount</Label>
                        <Input
                          id="principal_payment"
                          type="number"
                          step="0.01"
                          placeholder="0.00"
                          value={paymentFormData.principal_amount}
                          onChange={(e) => setPaymentFormData({ ...paymentFormData, principal_amount: e.target.value })}
                        />
                      </div>
                      <div className="space-y-2">
                        <Label htmlFor="interest_payment">Interest Amount</Label>
                        <Input
                          id="interest_payment"
                          type="number"
                          step="0.01"
                          placeholder="0.00"
                          value={paymentFormData.interest_amount}
                          onChange={(e) => setPaymentFormData({ ...paymentFormData, interest_amount: e.target.value })}
                        />
                      </div>
                    </div>

                    <div className="space-y-2">
                      <Label htmlFor="payment_date">Payment Date *</Label>
                      <Input
                        id="payment_date"
                        type="date"
                        value={paymentFormData.payment_date}
                        onChange={(e) => setPaymentFormData({ ...paymentFormData, payment_date: e.target.value })}
                        required
                      />
                    </div>

                    <div className="space-y-2">
                      <Label htmlFor="payment_method">Payment Method</Label>
                      <Select
                        value={paymentFormData.payment_method}
                        onValueChange={(value) => setPaymentFormData({ ...paymentFormData, payment_method: value })}
                      >
                        <SelectTrigger>
                          <SelectValue placeholder="Select payment method" />
                        </SelectTrigger>
                        <SelectContent>
                          {paymentMethods.map((method) => (
                            <SelectItem key={method} value={method.toLowerCase().replace(" ", "_")}>
                              {method}
                            </SelectItem>
                          ))}
                        </SelectContent>
                      </Select>
                    </div>

                    <div className="space-y-2">
                      <Label htmlFor="payment_notes">Notes (Optional)</Label>
                      <Textarea
                        id="payment_notes"
                        placeholder="Additional notes about this payment"
                        value={paymentFormData.notes}
                        onChange={(e) => setPaymentFormData({ ...paymentFormData, notes: e.target.value })}
                      />
                    </div>

                    <div className="flex justify-end space-x-2">
                      <Button type="button" variant="outline" onClick={() => setPaymentDialogOpen(false)}>
                        Cancel
                      </Button>
                      <Button type="submit">Record Payment</Button>
                    </div>
                  </form>
                </DialogContent>
              </Dialog>
            </div>
          </div>
        </CardHeader>
        <CardContent>
          {loans.length === 0 ? (
            <div className="text-center py-12">
              <IndianRupee className="mx-auto h-12 w-12 text-muted-foreground mb-4" />
              <h3 className="text-lg font-semibold text-gray-900 mb-2">No loans recorded</h3>
              <p className="text-sm text-gray-500 mb-4">
                Get started by adding your first loan to track in INR or USD.
              </p>
              <Button onClick={() => setLoanDialogOpen(true)}>
                <Plus className="mr-2 h-4 w-4" />
                Add Your First Loan
              </Button>
            </div>
          ) : (
            <div className="space-y-6">
              {loans.map((loan) => {
                const progress = ((loan.principal_amount - loan.current_balance) / loan.principal_amount) * 100
                const loanPayments = payments.filter((p) => p.loan_id === loan.id)
                const loanCurrency = loan.currency || "INR"

                return (
                  <Card key={loan.id} className="border-l-4 border-l-blue-500">
                    <CardHeader>
                      <div className="flex items-center justify-between">
                        <div>
                          <CardTitle className="flex items-center gap-2">
                            {loan.loan_name}
                            <Badge
                              variant={
                                loan.status === "active"
                                  ? "default"
                                  : loan.status === "paid_off"
                                    ? "secondary"
                                    : "destructive"
                              }
                            >
                              {loan.status?.replace("_", " ").toUpperCase()}
                            </Badge>
                            <Badge variant="outline">{loanCurrency}</Badge>
                          </CardTitle>
                          <CardDescription>
                            {loan.lender && `${loan.lender} • `}
                            {loan.loan_type &&
                              `${loan.loan_type.charAt(0).toUpperCase() + loan.loan_type.slice(1).replace("_", " ")} Loan`}
                          </CardDescription>
                        </div>
                        <div className="flex space-x-2">
                          <Button variant="ghost" size="sm" onClick={() => handleEditLoan(loan)}>
                            <Edit className="h-4 w-4" />
                          </Button>
                          <Button variant="ghost" size="sm" onClick={() => handleDeleteLoan(loan.id)}>
                            <Trash2 className="h-4 w-4" />
                          </Button>
                        </div>
                      </div>
                    </CardHeader>
                    <CardContent className="space-y-4">
                      <div className="grid grid-cols-2 md:grid-cols-4 gap-4 text-sm">
                        <div>
                          <p className="text-muted-foreground">Original Amount</p>
                          <p className="font-semibold">
                            {loanCurrency === "INR"
                              ? formatINR(loan.principal_amount)
                              : formatUSD(loan.principal_amount)}
                          </p>
                        </div>
                        <div>
                          <p className="text-muted-foreground">Current Balance</p>
                          <p className="font-semibold text-red-600">
                            {loanCurrency === "INR" ? formatINR(loan.current_balance) : formatUSD(loan.current_balance)}
                          </p>
                        </div>
                        <div>
                          <p className="text-muted-foreground">Interest Rate</p>
                          <p className="font-semibold">{loan.interest_rate ? `${loan.interest_rate}% p.a.` : "N/A"}</p>
                        </div>
                        <div>
                          <p className="text-muted-foreground">Monthly EMI</p>
                          <p className="font-semibold">
                            {loan.monthly_payment
                              ? loanCurrency === "INR"
                                ? formatINR(loan.monthly_payment)
                                : formatUSD(loan.monthly_payment)
                              : "N/A"}
                          </p>
                        </div>
                      </div>

                      <div className="space-y-2">
                        <div className="flex justify-between items-center text-sm">
                          <span>Repayment Progress</span>
                          <span className="font-medium">{progress.toFixed(1)}% paid off</span>
                        </div>
                        <Progress value={progress} className="h-2" />
                        <div className="flex justify-between text-xs text-muted-foreground">
                          <span>
                            Paid:{" "}
                            {loanCurrency === "INR"
                              ? formatINR(loan.principal_amount - loan.current_balance)
                              : formatUSD(loan.principal_amount - loan.current_balance)}
                          </span>
                          <span>
                            Remaining:{" "}
                            {loanCurrency === "INR" ? formatINR(loan.current_balance) : formatUSD(loan.current_balance)}
                          </span>
                        </div>
                      </div>

                      {loanPayments.length > 0 && (
                        <div className="pt-4 border-t">
                          <h4 className="text-sm font-medium mb-2">Recent Payments</h4>
                          <div className="space-y-2">
                            {loanPayments.slice(0, 3).map((payment) => (
                              <div key={payment.id} className="flex justify-between items-center text-sm">
                                <span>{new Date(payment.payment_date).toLocaleDateString("en-IN")}</span>
                                <span className="font-medium text-green-600">
                                  {loanCurrency === "INR" ? formatINR(payment.amount) : formatUSD(payment.amount)}
                                </span>
                              </div>
                            ))}
                            {loanPayments.length > 3 && (
                              <p className="text-xs text-muted-foreground">+{loanPayments.length - 3} more payments</p>
                            )}
                          </div>
                        </div>
                      )}
                    </CardContent>
                  </Card>
                )
              })}
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  )
}
