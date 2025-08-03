"use client"

import type React from "react"

import { useEffect, useState } from "react"
import { supabase } from "@/lib/supabase"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Badge } from "@/components/ui/badge"
import { Alert, AlertDescription } from "@/components/ui/alert"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import {
  Plus,
  CreditCard,
  TrendingDown,
  Calendar,
  DollarSign,
  Edit,
  Trash2,
  AlertCircle,
  Info,
  IndianRupee,
} from "lucide-react"
import { formatINR, convertCurrency } from "@/lib/utils"

interface Loan {
  id: string
  loan_name: string
  principal_amount: number
  current_balance: number
  interest_rate: number
  currency: "INR" | "USD"
  status: "active" | "paid_off"
  created_at: string
}

interface Payment {
  id: string
  loan_id: string
  amount: number
  principal_amount: number
  interest_amount: number
  payment_date: string
  notes?: string
  loan?: Loan
}

export default function LoanTracker() {
  const [loans, setLoans] = useState<Loan[]>([])
  const [payments, setPayments] = useState<Payment[]>([])
  const [totalInterestPaid, setTotalInterestPaid] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState("")
  const [success, setSuccess] = useState("")

  // Form states
  const [isAddingLoan, setIsAddingLoan] = useState(false)
  const [isAddingPayment, setIsAddingPayment] = useState(false)
  const [editingPayment, setEditingPayment] = useState<Payment | null>(null)
  const [loanForm, setLoanForm] = useState({
    loan_name: "",
    principal_amount: "",
    interest_rate: "",
    currency: "INR" as "INR" | "USD",
  })
  const [paymentForm, setPaymentForm] = useState({
    loan_id: "",
    total_amount: "",
    principal_amount: "",
    interest_amount: "",
    payment_date: new Date().toISOString().split("T")[0],
    notes: "",
  })

  useEffect(() => {
    fetchData()
  }, [])

  useEffect(() => {
    // Auto-calculate total amount when principal or interest changes
    const principal = Number.parseFloat(paymentForm.principal_amount) || 0
    const interest = Number.parseFloat(paymentForm.interest_amount) || 0
    const total = principal + interest

    if (total > 0) {
      setPaymentForm((prev) => ({
        ...prev,
        total_amount: total.toString(),
      }))
    }
  }, [paymentForm.principal_amount, paymentForm.interest_amount])

  const fetchData = async () => {
    try {
      setError("")
      const {
        data: { user },
      } = await supabase.auth.getUser()

      if (!user) {
        setError("Please log in to view loans")
        return
      }

      // Fetch loans
      const { data: loansData, error: loansError } = await supabase
        .from("loans")
        .select("*")
        .eq("user_id", user.id)
        .order("created_at", { ascending: false })

      if (loansError) {
        console.error("Error fetching loans:", loansError)
        setError("Failed to fetch loans")
        return
      }

      // Fetch payments with loan details
      const { data: paymentsData, error: paymentsError } = await supabase
        .from("loan_payments")
        .select(`
          *,
          loan:loans(*)
        `)
        .eq("user_id", user.id)
        .order("payment_date", { ascending: false })

      if (paymentsError) {
        console.error("Error fetching payments:", paymentsError)
        setError("Failed to fetch payments")
        return
      }

      setLoans(loansData || [])
      setPayments(paymentsData || [])

      // Calculate total interest paid (convert to INR for consistency)
      const totalInterest = (paymentsData || []).reduce((sum, payment) => {
        const interestInINR =
          payment.loan?.currency === "USD"
            ? convertCurrency(payment.interest_amount, "USD", "INR")
            : payment.interest_amount
        return sum + interestInINR
      }, 0)
      setTotalInterestPaid(totalInterest)
    } catch (error: any) {
      console.error("Error fetching data:", error)
      setError(error.message || "Failed to fetch data")
    } finally {
      setLoading(false)
    }
  }

  const handleAddLoan = async (e: React.FormEvent) => {
    e.preventDefault()
    setLoading(true)
    setError("")

    try {
      const {
        data: { user },
      } = await supabase.auth.getUser()

      if (!user) {
        setError("Please log in to add loans")
        return
      }

      const { error } = await supabase.from("loans").insert([
        {
          user_id: user.id,
          loan_name: loanForm.loan_name,
          principal_amount: Number.parseFloat(loanForm.principal_amount),
          current_balance: Number.parseFloat(loanForm.principal_amount),
          interest_rate: Number.parseFloat(loanForm.interest_rate),
          currency: loanForm.currency,
          status: "active",
        },
      ])

      if (error) {
        setError(error.message)
      } else {
        setSuccess("Loan added successfully!")
        setIsAddingLoan(false)
        setLoanForm({
          loan_name: "",
          principal_amount: "",
          interest_rate: "",
          currency: "INR",
        })
        fetchData()
      }
    } catch (error: any) {
      setError(error.message || "Failed to add loan")
    } finally {
      setLoading(false)
    }
  }

  const handleAddPayment = async (e: React.FormEvent) => {
    e.preventDefault()
    setLoading(true)
    setError("")

    try {
      const {
        data: { user },
      } = await supabase.auth.getUser()

      if (!user) {
        setError("Please log in to add payments")
        return
      }

      const principalAmount = Number.parseFloat(paymentForm.principal_amount) || 0
      const interestAmount = Number.parseFloat(paymentForm.interest_amount) || 0
      const totalAmount = principalAmount + interestAmount

      // Validate amounts
      if (totalAmount <= 0) {
        setError("Payment amount must be greater than 0")
        return
      }

      if (Number.parseFloat(paymentForm.total_amount) !== totalAmount) {
        setError("Total amount must equal principal + interest")
        return
      }

      // Get current loan balance
      const { data: loanData, error: loanError } = await supabase
        .from("loans")
        .select("current_balance")
        .eq("id", paymentForm.loan_id)
        .single()

      if (loanError) {
        setError("Failed to fetch loan details")
        return
      }

      if (principalAmount > loanData.current_balance) {
        setError("Principal payment cannot exceed current loan balance")
        return
      }

      // Add payment record
      const { error: paymentError } = await supabase.from("loan_payments").insert([
        {
          user_id: user.id,
          loan_id: paymentForm.loan_id,
          amount: totalAmount,
          principal_amount: principalAmount,
          interest_amount: interestAmount,
          payment_date: paymentForm.payment_date,
          notes: paymentForm.notes || null,
        },
      ])

      if (paymentError) {
        setError(paymentError.message)
        return
      }

      // Update loan balance (only reduce by principal amount)
      const newBalance = loanData.current_balance - principalAmount
      const newStatus = newBalance <= 0 ? "paid_off" : "active"

      const { error: updateError } = await supabase
        .from("loans")
        .update({
          current_balance: Math.max(0, newBalance),
          status: newStatus,
        })
        .eq("id", paymentForm.loan_id)

      if (updateError) {
        setError(updateError.message)
        return
      }

      setSuccess("Payment recorded successfully!")
      setIsAddingPayment(false)
      setPaymentForm({
        loan_id: "",
        total_amount: "",
        principal_amount: "",
        interest_amount: "",
        payment_date: new Date().toISOString().split("T")[0],
        notes: "",
      })
      fetchData()
    } catch (error: any) {
      setError(error.message || "Failed to record payment")
    } finally {
      setLoading(false)
    }
  }

  const handleEditPayment = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!editingPayment) return

    setLoading(true)
    setError("")

    try {
      const principalAmount = Number.parseFloat(paymentForm.principal_amount) || 0
      const interestAmount = Number.parseFloat(paymentForm.interest_amount) || 0
      const totalAmount = principalAmount + interestAmount

      // Validate amounts
      if (totalAmount <= 0) {
        setError("Payment amount must be greater than 0")
        return
      }

      // Calculate balance adjustment
      const oldPrincipal = editingPayment.principal_amount
      const newPrincipal = principalAmount
      const principalDifference = newPrincipal - oldPrincipal

      // Get current loan balance
      const { data: loanData, error: loanError } = await supabase
        .from("loans")
        .select("current_balance")
        .eq("id", editingPayment.loan_id)
        .single()

      if (loanError) {
        setError("Failed to fetch loan details")
        return
      }

      // Check if the adjustment would make balance negative
      if (loanData.current_balance - principalDifference < 0) {
        setError("This change would result in a negative loan balance")
        return
      }

      // Update payment record
      const { error: paymentError } = await supabase
        .from("loan_payments")
        .update({
          amount: totalAmount,
          principal_amount: principalAmount,
          interest_amount: interestAmount,
          payment_date: paymentForm.payment_date,
          notes: paymentForm.notes || null,
        })
        .eq("id", editingPayment.id)

      if (paymentError) {
        setError(paymentError.message)
        return
      }

      // Update loan balance
      const newBalance = loanData.current_balance - principalDifference
      const newStatus = newBalance <= 0 ? "paid_off" : "active"

      const { error: updateError } = await supabase
        .from("loans")
        .update({
          current_balance: Math.max(0, newBalance),
          status: newStatus,
        })
        .eq("id", editingPayment.loan_id)

      if (updateError) {
        setError(updateError.message)
        return
      }

      setSuccess("Payment updated successfully!")
      setEditingPayment(null)
      setPaymentForm({
        loan_id: "",
        total_amount: "",
        principal_amount: "",
        interest_amount: "",
        payment_date: new Date().toISOString().split("T")[0],
        notes: "",
      })
      fetchData()
    } catch (error: any) {
      setError(error.message || "Failed to update payment")
    } finally {
      setLoading(false)
    }
  }

  const handleDeletePayment = async (payment: Payment) => {
    if (!confirm("Are you sure you want to delete this payment?")) return

    setLoading(true)
    setError("")

    try {
      // Delete payment record
      const { error: deleteError } = await supabase.from("loan_payments").delete().eq("id", payment.id)

      if (deleteError) {
        setError(deleteError.message)
        return
      }

      // Restore loan balance (add back the principal amount)
      const { data: loanData, error: loanError } = await supabase
        .from("loans")
        .select("current_balance")
        .eq("id", payment.loan_id)
        .single()

      if (loanError) {
        setError("Failed to fetch loan details")
        return
      }

      const newBalance = loanData.current_balance + payment.principal_amount

      const { error: updateError } = await supabase
        .from("loans")
        .update({
          current_balance: newBalance,
          status: "active", // Restore to active since we're adding balance back
        })
        .eq("id", payment.loan_id)

      if (updateError) {
        setError(updateError.message)
        return
      }

      setSuccess("Payment deleted successfully!")
      fetchData()
    } catch (error: any) {
      setError(error.message || "Failed to delete payment")
    } finally {
      setLoading(false)
    }
  }

  const startEditPayment = (payment: Payment) => {
    setEditingPayment(payment)
    setPaymentForm({
      loan_id: payment.loan_id,
      total_amount: payment.amount.toString(),
      principal_amount: payment.principal_amount.toString(),
      interest_amount: payment.interest_amount.toString(),
      payment_date: payment.payment_date,
      notes: payment.notes || "",
    })
  }

  const getTotalDebt = () => {
    return loans.reduce((sum, loan) => {
      const balanceInINR =
        loan.currency === "USD" ? convertCurrency(loan.current_balance, "USD", "INR") : loan.current_balance
      return sum + balanceInINR
    }, 0)
  }

  if (loading && loans.length === 0) {
    return (
      <div className="space-y-6">
        <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
          {[...Array(3)].map((_, i) => (
            <Card key={i}>
              <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                <div className="h-4 bg-muted rounded w-24 animate-pulse" />
                <div className="h-4 w-4 bg-muted rounded animate-pulse" />
              </CardHeader>
              <CardContent>
                <div className="h-8 bg-muted rounded w-32 animate-pulse mb-2" />
                <div className="h-3 bg-muted rounded w-20 animate-pulse" />
              </CardContent>
            </Card>
          ))}
        </div>
      </div>
    )
  }

  return (
    <div className="space-y-6 p-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold tracking-tight">Loan Management</h1>
          <p className="text-muted-foreground">Track your loans and payments</p>
        </div>
        <div className="flex gap-2">
          <Dialog open={isAddingLoan} onOpenChange={setIsAddingLoan}>
            <DialogTrigger asChild>
              <Button>
                <Plus className="mr-2 h-4 w-4" />
                Add Loan
              </Button>
            </DialogTrigger>
            <DialogContent>
              <DialogHeader>
                <DialogTitle>Add New Loan</DialogTitle>
                <DialogDescription>Enter the details of your new loan</DialogDescription>
              </DialogHeader>
              <form onSubmit={handleAddLoan} className="space-y-4">
                <div className="space-y-2">
                  <Label htmlFor="loan_name">Loan Name</Label>
                  <Input
                    id="loan_name"
                    value={loanForm.loan_name}
                    onChange={(e) => setLoanForm({ ...loanForm, loan_name: e.target.value })}
                    placeholder="e.g., Home Loan, Car Loan"
                    required
                  />
                </div>
                <div className="grid grid-cols-2 gap-4">
                  <div className="space-y-2">
                    <Label htmlFor="principal_amount">Principal Amount</Label>
                    <Input
                      id="principal_amount"
                      type="number"
                      step="0.01"
                      value={loanForm.principal_amount}
                      onChange={(e) => setLoanForm({ ...loanForm, principal_amount: e.target.value })}
                      placeholder="0.00"
                      required
                    />
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="currency">Currency</Label>
                    <Select
                      value={loanForm.currency}
                      onValueChange={(value: "INR" | "USD") => setLoanForm({ ...loanForm, currency: value })}
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
                <div className="space-y-2">
                  <Label htmlFor="interest_rate">Interest Rate (%)</Label>
                  <Input
                    id="interest_rate"
                    type="number"
                    step="0.01"
                    value={loanForm.interest_rate}
                    onChange={(e) => setLoanForm({ ...loanForm, interest_rate: e.target.value })}
                    placeholder="0.00"
                    required
                  />
                </div>
                <DialogFooter>
                  <Button type="button" variant="outline" onClick={() => setIsAddingLoan(false)}>
                    Cancel
                  </Button>
                  <Button type="submit" disabled={loading}>
                    Add Loan
                  </Button>
                </DialogFooter>
              </form>
            </DialogContent>
          </Dialog>

          <Dialog open={isAddingPayment} onOpenChange={setIsAddingPayment}>
            <DialogTrigger asChild>
              <Button variant="outline">
                <DollarSign className="mr-2 h-4 w-4" />
                Record Payment
              </Button>
            </DialogTrigger>
            <DialogContent className="max-w-md">
              <DialogHeader>
                <DialogTitle>Record Loan Payment</DialogTitle>
                <DialogDescription>Enter payment details</DialogDescription>
              </DialogHeader>

              <Alert>
                <Info className="h-4 w-4" />
                <AlertDescription>
                  <strong>Principal</strong> reduces your loan balance. <strong>Interest</strong> is tracked separately
                  and doesn't reduce the balance.
                </AlertDescription>
              </Alert>

              <form onSubmit={handleAddPayment} className="space-y-4">
                <div className="space-y-2">
                  <Label htmlFor="loan_select">Select Loan</Label>
                  <Select
                    value={paymentForm.loan_id}
                    onValueChange={(value) => setPaymentForm({ ...paymentForm, loan_id: value })}
                    required
                  >
                    <SelectTrigger>
                      <SelectValue placeholder="Choose a loan" />
                    </SelectTrigger>
                    <SelectContent>
                      {loans
                        .filter((loan) => loan.status === "active")
                        .map((loan) => (
                          <SelectItem key={loan.id} value={loan.id}>
                            {loan.loan_name} - {loan.currency === "INR" ? "₹" : "$"}
                            {loan.current_balance.toLocaleString()}
                          </SelectItem>
                        ))}
                    </SelectContent>
                  </Select>
                </div>

                <div className="grid grid-cols-2 gap-4">
                  <div className="space-y-2">
                    <Label htmlFor="principal_amount">
                      Principal Amount
                      <Badge variant="secondary" className="ml-2 text-xs">
                        Reduces Balance
                      </Badge>
                    </Label>
                    <Input
                      id="principal_amount"
                      type="number"
                      step="0.01"
                      value={paymentForm.principal_amount}
                      onChange={(e) => setPaymentForm({ ...paymentForm, principal_amount: e.target.value })}
                      placeholder="0.00"
                    />
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="interest_amount">
                      Interest Amount
                      <Badge variant="outline" className="ml-2 text-xs text-orange-600 border-orange-200">
                        Interest Only
                      </Badge>
                    </Label>
                    <Input
                      id="interest_amount"
                      type="number"
                      step="0.01"
                      value={paymentForm.interest_amount}
                      onChange={(e) => setPaymentForm({ ...paymentForm, interest_amount: e.target.value })}
                      placeholder="0.00"
                    />
                  </div>
                </div>

                <div className="space-y-2">
                  <Label htmlFor="total_amount">Total Payment Amount</Label>
                  <Input
                    id="total_amount"
                    type="number"
                    step="0.01"
                    value={paymentForm.total_amount}
                    onChange={(e) => setPaymentForm({ ...paymentForm, total_amount: e.target.value })}
                    placeholder="0.00"
                    required
                  />
                </div>

                <div className="space-y-2">
                  <Label htmlFor="payment_date">Payment Date</Label>
                  <Input
                    id="payment_date"
                    type="date"
                    value={paymentForm.payment_date}
                    onChange={(e) => setPaymentForm({ ...paymentForm, payment_date: e.target.value })}
                    required
                  />
                </div>

                <div className="space-y-2">
                  <Label htmlFor="notes">Notes (Optional)</Label>
                  <Input
                    id="notes"
                    value={paymentForm.notes}
                    onChange={(e) => setPaymentForm({ ...paymentForm, notes: e.target.value })}
                    placeholder="Payment notes..."
                  />
                </div>

                <DialogFooter>
                  <Button type="button" variant="outline" onClick={() => setIsAddingPayment(false)}>
                    Cancel
                  </Button>
                  <Button type="submit" disabled={loading}>
                    Record Payment
                  </Button>
                </DialogFooter>
              </form>
            </DialogContent>
          </Dialog>
        </div>
      </div>

      {/* Error and Success Messages */}
      {error && (
        <Alert variant="destructive">
          <AlertCircle className="h-4 w-4" />
          <AlertDescription>{error}</AlertDescription>
        </Alert>
      )}

      {success && (
        <Alert>
          <AlertCircle className="h-4 w-4" />
          <AlertDescription>{success}</AlertDescription>
        </Alert>
      )}

      {/* Summary Cards */}
      <div className="grid gap-4 md:grid-cols-3">
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Total Debt</CardTitle>
            <CreditCard className="h-4 w-4 text-red-600" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold text-red-600">{formatINR(getTotalDebt())}</div>
            <p className="text-xs text-muted-foreground">
              {loans.filter((loan) => loan.status === "active").length} active loans
            </p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Total Interest Paid</CardTitle>
            <TrendingDown className="h-4 w-4 text-orange-600" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold text-orange-600">{formatINR(totalInterestPaid)}</div>
            <p className="text-xs text-muted-foreground">From {payments.length} payments</p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Active Loans</CardTitle>
            <Calendar className="h-4 w-4 text-blue-600" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold text-blue-600">
              {loans.filter((loan) => loan.status === "active").length}
            </div>
            <p className="text-xs text-muted-foreground">
              {loans.filter((loan) => loan.status === "paid_off").length} paid off
            </p>
          </CardContent>
        </Card>
      </div>

      {/* Loans List */}
      <Card>
        <CardHeader>
          <CardTitle>Your Loans</CardTitle>
          <CardDescription>Overview of all your loans and their current status</CardDescription>
        </CardHeader>
        <CardContent>
          {loans.length === 0 ? (
            <div className="text-center py-8">
              <CreditCard className="mx-auto h-12 w-12 text-muted-foreground" />
              <h3 className="mt-4 text-lg font-semibold">No loans found</h3>
              <p className="text-muted-foreground">Add your first loan to start tracking</p>
            </div>
          ) : (
            <div className="space-y-4">
              {loans.map((loan) => (
                <div key={loan.id} className="flex items-center justify-between p-4 border rounded-lg">
                  <div className="flex items-center space-x-4">
                    <div className="flex-shrink-0">
                      <div className="w-10 h-10 bg-primary/10 rounded-full flex items-center justify-center">
                        <CreditCard className="h-5 w-5 text-primary" />
                      </div>
                    </div>
                    <div>
                      <h3 className="font-medium">{loan.loan_name}</h3>
                      <div className="flex items-center gap-2 text-sm text-muted-foreground">
                        <span>
                          {loan.currency === "INR" ? "₹" : "$"}
                          {loan.current_balance.toLocaleString()} remaining
                        </span>
                        <Badge variant={loan.status === "active" ? "default" : "secondary"}>{loan.status}</Badge>
                      </div>
                    </div>
                  </div>
                  <div className="text-right">
                    <div className="text-sm font-medium">{loan.interest_rate}% interest</div>
                    <div className="text-xs text-muted-foreground">
                      Principal: {loan.currency === "INR" ? "₹" : "$"}
                      {loan.principal_amount.toLocaleString()}
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </CardContent>
      </Card>

      {/* Recent Payments */}
      <Card>
        <CardHeader>
          <CardTitle>Recent Payments</CardTitle>
          <CardDescription>Your latest loan payments and their breakdown</CardDescription>
        </CardHeader>
        <CardContent>
          {payments.length === 0 ? (
            <div className="text-center py-8">
              <DollarSign className="mx-auto h-12 w-12 text-muted-foreground" />
              <h3 className="mt-4 text-lg font-semibold">No payments recorded</h3>
              <p className="text-muted-foreground">Record your first payment to see it here</p>
            </div>
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Loan</TableHead>
                  <TableHead>Date</TableHead>
                  <TableHead>Principal</TableHead>
                  <TableHead>Interest</TableHead>
                  <TableHead>Total</TableHead>
                  <TableHead>Actions</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {payments.slice(0, 10).map((payment) => (
                  <TableRow key={payment.id}>
                    <TableCell className="font-medium">{payment.loan?.loan_name || "Unknown Loan"}</TableCell>
                    <TableCell>{new Date(payment.payment_date).toLocaleDateString()}</TableCell>
                    <TableCell>
                      <div className="flex items-center gap-1">
                        {payment.loan?.currency === "INR" ? (
                          <IndianRupee className="h-3 w-3" />
                        ) : (
                          <DollarSign className="h-3 w-3" />
                        )}
                        {payment.principal_amount.toLocaleString()}
                        <Badge variant="secondary" className="text-xs">
                          Reduces Balance
                        </Badge>
                      </div>
                    </TableCell>
                    <TableCell>
                      <div className="flex items-center gap-1">
                        {payment.loan?.currency === "INR" ? (
                          <IndianRupee className="h-3 w-3" />
                        ) : (
                          <DollarSign className="h-3 w-3" />
                        )}
                        {payment.interest_amount.toLocaleString()}
                        {payment.interest_amount > 0 && (
                          <Badge variant="outline" className="text-xs text-orange-600 border-orange-200">
                            Interest
                          </Badge>
                        )}
                      </div>
                    </TableCell>
                    <TableCell className="font-medium">
                      {payment.loan?.currency === "INR" ? "₹" : "$"}
                      {payment.amount.toLocaleString()}
                    </TableCell>
                    <TableCell>
                      <div className="flex items-center gap-2">
                        <Button variant="ghost" size="sm" onClick={() => startEditPayment(payment)}>
                          <Edit className="h-4 w-4" />
                        </Button>
                        <Button variant="ghost" size="sm" onClick={() => handleDeletePayment(payment)}>
                          <Trash2 className="h-4 w-4" />
                        </Button>
                      </div>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}
        </CardContent>
      </Card>

      {/* Edit Payment Dialog */}
      <Dialog open={!!editingPayment} onOpenChange={() => setEditingPayment(null)}>
        <DialogContent className="max-w-md">
          <DialogHeader>
            <DialogTitle>Edit Payment</DialogTitle>
            <DialogDescription>Update payment details</DialogDescription>
          </DialogHeader>

          <Alert>
            <Info className="h-4 w-4" />
            <AlertDescription>
              <strong>Principal</strong> reduces your loan balance. <strong>Interest</strong> is tracked separately.
            </AlertDescription>
          </Alert>

          <form onSubmit={handleEditPayment} className="space-y-4">
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label htmlFor="edit_principal_amount">
                  Principal Amount
                  <Badge variant="secondary" className="ml-2 text-xs">
                    Reduces Balance
                  </Badge>
                </Label>
                <Input
                  id="edit_principal_amount"
                  type="number"
                  step="0.01"
                  value={paymentForm.principal_amount}
                  onChange={(e) => setPaymentForm({ ...paymentForm, principal_amount: e.target.value })}
                  placeholder="0.00"
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="edit_interest_amount">
                  Interest Amount
                  <Badge variant="outline" className="ml-2 text-xs text-orange-600 border-orange-200">
                    Interest Only
                  </Badge>
                </Label>
                <Input
                  id="edit_interest_amount"
                  type="number"
                  step="0.01"
                  value={paymentForm.interest_amount}
                  onChange={(e) => setPaymentForm({ ...paymentForm, interest_amount: e.target.value })}
                  placeholder="0.00"
                />
              </div>
            </div>

            <div className="space-y-2">
              <Label htmlFor="edit_total_amount">Total Payment Amount</Label>
              <Input
                id="edit_total_amount"
                type="number"
                step="0.01"
                value={paymentForm.total_amount}
                onChange={(e) => setPaymentForm({ ...paymentForm, total_amount: e.target.value })}
                placeholder="0.00"
                required
              />
            </div>

            <div className="space-y-2">
              <Label htmlFor="edit_payment_date">Payment Date</Label>
              <Input
                id="edit_payment_date"
                type="date"
                value={paymentForm.payment_date}
                onChange={(e) => setPaymentForm({ ...paymentForm, payment_date: e.target.value })}
                required
              />
            </div>

            <div className="space-y-2">
              <Label htmlFor="edit_notes">Notes (Optional)</Label>
              <Input
                id="edit_notes"
                value={paymentForm.notes}
                onChange={(e) => setPaymentForm({ ...paymentForm, notes: e.target.value })}
                placeholder="Payment notes..."
              />
            </div>

            <DialogFooter>
              <Button type="button" variant="outline" onClick={() => setEditingPayment(null)}>
                Cancel
              </Button>
              <Button type="submit" disabled={loading}>
                Update Payment
              </Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>
    </div>
  )
}
