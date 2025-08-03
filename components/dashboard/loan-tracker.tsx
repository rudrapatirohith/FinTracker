"use client"

import type React from "react"

import { useState, useEffect } from "react"
import { supabase } from "@/lib/supabase"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
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
import { Textarea } from "@/components/ui/textarea"
import { Trash2, Plus, CreditCard, DollarSign, Calendar, Info } from "lucide-react"
import { formatCurrency } from "@/lib/utils"

interface Loan {
  id: string
  loan_name: string
  principal_amount: number
  current_balance: number
  interest_rate: number
  currency: string
  loan_type: string
  start_date: string
  end_date: string
  description?: string
  created_at: string
}

interface Payment {
  id: string
  loan_id: string
  amount: number
  principal_amount: number
  interest_amount: number
  payment_date: string
  description?: string
  created_at: string
}

export default function LoanTracker() {
  const [loans, setLoans] = useState<Loan[]>([])
  const [payments, setPayments] = useState<Payment[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState("")
  const [success, setSuccess] = useState("")

  // Form states
  const [isAddingLoan, setIsAddingLoan] = useState(false)
  const [isAddingPayment, setIsAddingPayment] = useState(false)
  const [editingLoan, setEditingLoan] = useState<Loan | null>(null)
  const [editingPayment, setEditingPayment] = useState<Payment | null>(null)
  const [selectedLoanId, setSelectedLoanId] = useState("")

  // Form data
  const [loanForm, setLoanForm] = useState({
    loan_name: "",
    principal_amount: "",
    interest_rate: "",
    currency: "USD",
    loan_type: "personal",
    start_date: "",
    end_date: "",
    description: "",
  })

  const [paymentForm, setPaymentForm] = useState({
    loan_id: "",
    amount: "",
    principal_amount: "",
    interest_amount: "",
    payment_date: new Date().toISOString().split("T")[0],
    description: "",
  })

  useEffect(() => {
    fetchLoans()
    fetchPayments()
  }, [])

  useEffect(() => {
    // Auto-calculate total amount when principal or interest changes
    const principal = Number.parseFloat(paymentForm.principal_amount) || 0
    const interest = Number.parseFloat(paymentForm.interest_amount) || 0
    const total = principal + interest

    if (total > 0) {
      setPaymentForm((prev) => ({
        ...prev,
        amount: total.toString(),
      }))
    }
  }, [paymentForm.principal_amount, paymentForm.interest_amount])

  const fetchLoans = async () => {
    try {
      const { data, error } = await supabase.from("loans").select("*").order("created_at", { ascending: false })

      if (error) throw error
      setLoans(data || [])
    } catch (err: any) {
      setError(err.message)
    }
  }

  const fetchPayments = async () => {
    try {
      const { data, error } = await supabase
        .from("loan_payments")
        .select("*")
        .order("payment_date", { ascending: false })

      if (error) throw error
      setPayments(data || [])
    } catch (err: any) {
      setError(err.message)
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
      if (!user) throw new Error("User not authenticated")

      const loanData = {
        user_id: user.id,
        loan_name: loanForm.loan_name,
        principal_amount: Number.parseFloat(loanForm.principal_amount),
        current_balance: Number.parseFloat(loanForm.principal_amount),
        interest_rate: Number.parseFloat(loanForm.interest_rate),
        currency: loanForm.currency,
        loan_type: loanForm.loan_type,
        start_date: loanForm.start_date,
        end_date: loanForm.end_date,
        description: loanForm.description,
      }

      const { error } = await supabase.from("loans").insert([loanData])

      if (error) throw error

      setSuccess("Loan added successfully!")
      setIsAddingLoan(false)
      setLoanForm({
        loan_name: "",
        principal_amount: "",
        interest_rate: "",
        currency: "USD",
        loan_type: "personal",
        start_date: "",
        end_date: "",
        description: "",
      })
      fetchLoans()
    } catch (err: any) {
      setError(err.message)
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
      if (!user) throw new Error("User not authenticated")

      const principalAmount = Number.parseFloat(paymentForm.principal_amount) || 0
      const interestAmount = Number.parseFloat(paymentForm.interest_amount) || 0
      const totalAmount = principalAmount + interestAmount

      // Validate that total matches the sum
      if (Math.abs(totalAmount - Number.parseFloat(paymentForm.amount)) > 0.01) {
        throw new Error("Total amount must equal Principal + Interest")
      }

      const paymentData = {
        user_id: user.id,
        loan_id: paymentForm.loan_id,
        amount: totalAmount,
        principal_amount: principalAmount,
        interest_amount: interestAmount,
        payment_date: paymentForm.payment_date,
        description: paymentForm.description,
      }

      const { error } = await supabase.from("loan_payments").insert([paymentData])

      if (error) throw error

      // Update loan balance - only reduce by principal amount
      const loan = loans.find((l) => l.id === paymentForm.loan_id)
      if (loan && principalAmount > 0) {
        const newBalance = Math.max(0, loan.current_balance - principalAmount)
        const { error: updateError } = await supabase
          .from("loans")
          .update({ current_balance: newBalance })
          .eq("id", paymentForm.loan_id)

        if (updateError) throw updateError
      }

      setSuccess("Payment recorded successfully!")
      setIsAddingPayment(false)
      setPaymentForm({
        loan_id: "",
        amount: "",
        principal_amount: "",
        interest_amount: "",
        payment_date: new Date().toISOString().split("T")[0],
        description: "",
      })
      fetchLoans()
      fetchPayments()
    } catch (err: any) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  const handleDeleteLoan = async (loanId: string) => {
    if (!confirm("Are you sure you want to delete this loan? This will also delete all associated payments.")) {
      return
    }

    try {
      setLoading(true)

      // Delete payments first
      const { error: paymentsError } = await supabase.from("loan_payments").delete().eq("loan_id", loanId)

      if (paymentsError) throw paymentsError

      // Delete loan
      const { error: loanError } = await supabase.from("loans").delete().eq("id", loanId)

      if (loanError) throw loanError

      setSuccess("Loan deleted successfully!")
      fetchLoans()
      fetchPayments()
    } catch (err: any) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  const handleDeletePayment = async (paymentId: string) => {
    if (!confirm("Are you sure you want to delete this payment?")) {
      return
    }

    try {
      setLoading(true)

      // Get payment details first
      const payment = payments.find((p) => p.id === paymentId)
      if (!payment) throw new Error("Payment not found")

      // Delete payment
      const { error } = await supabase.from("loan_payments").delete().eq("id", paymentId)

      if (error) throw error

      // Update loan balance - add back the principal amount
      const loan = loans.find((l) => l.id === payment.loan_id)
      if (loan && payment.principal_amount > 0) {
        const newBalance = loan.current_balance + payment.principal_amount
        const { error: updateError } = await supabase
          .from("loans")
          .update({ current_balance: newBalance })
          .eq("id", payment.loan_id)

        if (updateError) throw updateError
      }

      setSuccess("Payment deleted successfully!")
      fetchLoans()
      fetchPayments()
    } catch (err: any) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  const getTotalInterestPaid = () => {
    return payments.reduce((total, payment) => total + (payment.interest_amount || 0), 0)
  }

  const getPaymentsForLoan = (loanId: string) => {
    return payments.filter((payment) => payment.loan_id === loanId)
  }

  if (loading && loans.length === 0) {
    return (
      <div className="flex items-center justify-center p-8">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary"></div>
      </div>
    )
  }

  return (
    <div className="space-y-6 p-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold">Loan Management</h1>
          <p className="text-muted-foreground">Track your loans and payments</p>
        </div>
        <div className="flex gap-2">
          <Dialog open={isAddingLoan} onOpenChange={setIsAddingLoan}>
            <DialogTrigger asChild>
              <Button>
                <Plus className="h-4 w-4 mr-2" />
                Add Loan
              </Button>
            </DialogTrigger>
            <DialogContent className="max-w-md">
              <DialogHeader>
                <DialogTitle>Add New Loan</DialogTitle>
                <DialogDescription>Enter the details of your new loan.</DialogDescription>
              </DialogHeader>
              <form onSubmit={handleAddLoan} className="space-y-4">
                <div className="space-y-2">
                  <Label htmlFor="loan_name">Loan Name</Label>
                  <Input
                    id="loan_name"
                    value={loanForm.loan_name}
                    onChange={(e) => setLoanForm({ ...loanForm, loan_name: e.target.value })}
                    placeholder="e.g., Home Mortgage"
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
                      onValueChange={(value) => setLoanForm({ ...loanForm, currency: value })}
                    >
                      <SelectTrigger>
                        <SelectValue />
                      </SelectTrigger>
                      <SelectContent>
                        <SelectItem value="USD">USD</SelectItem>
                        <SelectItem value="INR">INR</SelectItem>
                        <SelectItem value="EUR">EUR</SelectItem>
                        <SelectItem value="GBP">GBP</SelectItem>
                      </SelectContent>
                    </Select>
                  </div>
                </div>
                <div className="grid grid-cols-2 gap-4">
                  <div className="space-y-2">
                    <Label htmlFor="interest_rate">Interest Rate (%)</Label>
                    <Input
                      id="interest_rate"
                      type="number"
                      step="0.01"
                      value={loanForm.interest_rate}
                      onChange={(e) => setLoanForm({ ...loanForm, interest_rate: e.target.value })}
                      placeholder="5.00"
                      required
                    />
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="loan_type">Loan Type</Label>
                    <Select
                      value={loanForm.loan_type}
                      onValueChange={(value) => setLoanForm({ ...loanForm, loan_type: value })}
                    >
                      <SelectTrigger>
                        <SelectValue />
                      </SelectTrigger>
                      <SelectContent>
                        <SelectItem value="personal">Personal</SelectItem>
                        <SelectItem value="mortgage">Mortgage</SelectItem>
                        <SelectItem value="auto">Auto</SelectItem>
                        <SelectItem value="student">Student</SelectItem>
                        <SelectItem value="business">Business</SelectItem>
                      </SelectContent>
                    </Select>
                  </div>
                </div>
                <div className="grid grid-cols-2 gap-4">
                  <div className="space-y-2">
                    <Label htmlFor="start_date">Start Date</Label>
                    <Input
                      id="start_date"
                      type="date"
                      value={loanForm.start_date}
                      onChange={(e) => setLoanForm({ ...loanForm, start_date: e.target.value })}
                      required
                    />
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="end_date">End Date</Label>
                    <Input
                      id="end_date"
                      type="date"
                      value={loanForm.end_date}
                      onChange={(e) => setLoanForm({ ...loanForm, end_date: e.target.value })}
                    />
                  </div>
                </div>
                <div className="space-y-2">
                  <Label htmlFor="description">Description (Optional)</Label>
                  <Textarea
                    id="description"
                    value={loanForm.description}
                    onChange={(e) => setLoanForm({ ...loanForm, description: e.target.value })}
                    placeholder="Additional notes about this loan"
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
                <DollarSign className="h-4 w-4 mr-2" />
                Record Payment
              </Button>
            </DialogTrigger>
            <DialogContent className="max-w-md">
              <DialogHeader>
                <DialogTitle>Record Loan Payment</DialogTitle>
                <DialogDescription>Record a payment made towards a loan.</DialogDescription>
              </DialogHeader>
              <form onSubmit={handleAddPayment} className="space-y-4">
                <div className="space-y-2">
                  <Label htmlFor="payment_loan_id">Select Loan</Label>
                  <Select
                    value={paymentForm.loan_id}
                    onValueChange={(value) => setPaymentForm({ ...paymentForm, loan_id: value })}
                  >
                    <SelectTrigger>
                      <SelectValue placeholder="Choose a loan" />
                    </SelectTrigger>
                    <SelectContent>
                      {loans.map((loan) => (
                        <SelectItem key={loan.id} value={loan.id}>
                          {loan.loan_name} - {formatCurrency(loan.current_balance, loan.currency)} remaining
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>

                <Alert>
                  <Info className="h-4 w-4" />
                  <AlertDescription>
                    <strong>Principal Amount:</strong> Reduces your loan balance
                    <br />
                    <strong>Interest Amount:</strong> Tracked separately, doesn't reduce balance
                  </AlertDescription>
                </Alert>

                <div className="grid grid-cols-2 gap-4">
                  <div className="space-y-2">
                    <Label htmlFor="principal_amount">Principal Amount</Label>
                    <Input
                      id="principal_amount"
                      type="number"
                      step="0.01"
                      value={paymentForm.principal_amount}
                      onChange={(e) => setPaymentForm({ ...paymentForm, principal_amount: e.target.value })}
                      placeholder="0.00"
                    />
                    <p className="text-xs text-muted-foreground">Reduces loan balance</p>
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="interest_amount">Interest Amount</Label>
                    <Input
                      id="interest_amount"
                      type="number"
                      step="0.01"
                      value={paymentForm.interest_amount}
                      onChange={(e) => setPaymentForm({ ...paymentForm, interest_amount: e.target.value })}
                      placeholder="0.00"
                    />
                    <p className="text-xs text-muted-foreground">Tracked separately</p>
                  </div>
                </div>

                <div className="space-y-2">
                  <Label htmlFor="total_amount">Total Payment Amount</Label>
                  <Input
                    id="total_amount"
                    type="number"
                    step="0.01"
                    value={paymentForm.amount}
                    onChange={(e) => setPaymentForm({ ...paymentForm, amount: e.target.value })}
                    placeholder="0.00"
                    required
                  />
                  <p className="text-xs text-muted-foreground">Should equal Principal + Interest</p>
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
                  <Label htmlFor="payment_description">Description (Optional)</Label>
                  <Textarea
                    id="payment_description"
                    value={paymentForm.description}
                    onChange={(e) => setPaymentForm({ ...paymentForm, description: e.target.value })}
                    placeholder="Payment notes"
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

      {error && (
        <Alert variant="destructive">
          <AlertDescription>{error}</AlertDescription>
        </Alert>
      )}

      {success && (
        <Alert>
          <AlertDescription>{success}</AlertDescription>
        </Alert>
      )}

      {/* Summary Cards */}
      <div className="grid gap-4 md:grid-cols-3">
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Total Loans</CardTitle>
            <CreditCard className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{loans.length}</div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Total Outstanding</CardTitle>
            <DollarSign className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">
              {loans
                .reduce((total, loan) => total + loan.current_balance, 0)
                .toLocaleString("en-US", {
                  style: "currency",
                  currency: "USD",
                })}
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Total Interest Paid</CardTitle>
            <Calendar className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">
              {getTotalInterestPaid().toLocaleString("en-US", {
                style: "currency",
                currency: "USD",
              })}
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Loans List */}
      <div className="grid gap-6">
        {loans.map((loan) => {
          const loanPayments = getPaymentsForLoan(loan.id)
          const totalPaid = loanPayments.reduce((sum, payment) => sum + payment.amount, 0)
          const interestPaid = loanPayments.reduce((sum, payment) => sum + (payment.interest_amount || 0), 0)

          return (
            <Card key={loan.id}>
              <CardHeader>
                <div className="flex items-center justify-between">
                  <div>
                    <CardTitle className="flex items-center gap-2">
                      {loan.loan_name}
                      <Badge variant="secondary">{loan.loan_type}</Badge>
                    </CardTitle>
                    <CardDescription>
                      {loan.interest_rate}% interest • Started {new Date(loan.start_date).toLocaleDateString()}
                    </CardDescription>
                  </div>
                  <div className="flex gap-2">
                    <Button variant="outline" size="sm" onClick={() => handleDeleteLoan(loan.id)}>
                      <Trash2 className="h-4 w-4" />
                    </Button>
                  </div>
                </div>
              </CardHeader>
              <CardContent>
                <div className="grid gap-4 md:grid-cols-4">
                  <div>
                    <p className="text-sm font-medium text-muted-foreground">Original Amount</p>
                    <p className="text-lg font-semibold">{formatCurrency(loan.principal_amount, loan.currency)}</p>
                  </div>
                  <div>
                    <p className="text-sm font-medium text-muted-foreground">Current Balance</p>
                    <p className="text-lg font-semibold">{formatCurrency(loan.current_balance, loan.currency)}</p>
                  </div>
                  <div>
                    <p className="text-sm font-medium text-muted-foreground">Total Paid</p>
                    <p className="text-lg font-semibold">{formatCurrency(totalPaid, loan.currency)}</p>
                  </div>
                  <div>
                    <p className="text-sm font-medium text-muted-foreground">Interest Paid</p>
                    <p className="text-lg font-semibold text-orange-600">
                      {formatCurrency(interestPaid, loan.currency)}
                    </p>
                  </div>
                </div>

                {loanPayments.length > 0 && (
                  <div className="mt-4">
                    <h4 className="text-sm font-medium mb-2">Recent Payments</h4>
                    <div className="space-y-2">
                      {loanPayments.slice(0, 3).map((payment) => (
                        <div key={payment.id} className="flex items-center justify-between p-2 bg-muted rounded">
                          <div className="flex items-center gap-2">
                            <span className="text-sm">
                              {formatCurrency(payment.amount, loan.currency)} on{" "}
                              {new Date(payment.payment_date).toLocaleDateString()}
                            </span>
                            {payment.interest_amount > 0 && (
                              <Badge variant="outline" className="text-orange-600 border-orange-200">
                                Interest: {formatCurrency(payment.interest_amount, loan.currency)}
                              </Badge>
                            )}
                          </div>
                          <Button variant="ghost" size="sm" onClick={() => handleDeletePayment(payment.id)}>
                            <Trash2 className="h-3 w-3" />
                          </Button>
                        </div>
                      ))}
                    </div>
                  </div>
                )}
              </CardContent>
            </Card>
          )
        })}
      </div>

      {loans.length === 0 && (
        <Card>
          <CardContent className="flex flex-col items-center justify-center py-12">
            <CreditCard className="h-12 w-12 text-muted-foreground mb-4" />
            <h3 className="text-lg font-semibold mb-2">No loans yet</h3>
            <p className="text-muted-foreground text-center mb-4">
              Start by adding your first loan to track payments and balances.
            </p>
            <Button onClick={() => setIsAddingLoan(true)}>
              <Plus className="h-4 w-4 mr-2" />
              Add Your First Loan
            </Button>
          </CardContent>
        </Card>
      )}
    </div>
  )
}
