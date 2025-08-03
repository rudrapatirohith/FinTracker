"use client"

import type React from "react"

import { useState, useEffect } from "react"
import { supabase } from "@/lib/supabase"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog"
import { Alert, AlertDescription } from "@/components/ui/alert"
import { Badge } from "@/components/ui/badge"
import { Plus, CreditCard, DollarSign, TrendingDown, Edit, Trash2, AlertCircle, Info } from "lucide-react"
import { formatCurrency } from "@/lib/utils"

interface Loan {
  id: string
  loan_name: string
  principal_amount: number
  current_balance: number
  interest_rate: number
  currency: string
  created_at: string
}

interface Payment {
  id: string
  loan_id: string
  amount: number
  principal_amount: number
  interest_amount: number
  payment_date: string
  description: string
  loans: {
    loan_name: string
    currency: string
  }
}

export default function LoanTracker() {
  const [loans, setLoans] = useState<Loan[]>([])
  const [payments, setPayments] = useState<Payment[]>([])
  const [loading, setLoading] = useState(true)
  const [isAddingLoan, setIsAddingLoan] = useState(false)
  const [isAddingPayment, setIsAddingPayment] = useState(false)
  const [editingPayment, setEditingPayment] = useState<Payment | null>(null)
  const [error, setError] = useState("")

  // Form states
  const [loanName, setLoanName] = useState("")
  const [principalAmount, setPrincipalAmount] = useState("")
  const [interestRate, setInterestRate] = useState("")
  const [currency, setCurrency] = useState("INR")

  // Payment form states
  const [selectedLoanId, setSelectedLoanId] = useState("")
  const [paymentAmount, setPaymentAmount] = useState("")
  const [principalPayment, setPrincipalPayment] = useState("")
  const [interestPayment, setInterestPayment] = useState("")
  const [paymentDescription, setPaymentDescription] = useState("")
  const [paymentDate, setPaymentDate] = useState(new Date().toISOString().split("T")[0])

  useEffect(() => {
    fetchLoans()
    fetchPayments()
  }, [])

  // Auto-calculate total when principal or interest changes
  useEffect(() => {
    const principal = Number.parseFloat(principalPayment) || 0
    const interest = Number.parseFloat(interestPayment) || 0
    const total = principal + interest
    setPaymentAmount(total > 0 ? total.toString() : "")
  }, [principalPayment, interestPayment])

  const fetchLoans = async () => {
    try {
      const {
        data: { user },
      } = await supabase.auth.getUser()
      if (!user) return

      const { data, error } = await supabase
        .from("loans")
        .select("*")
        .eq("user_id", user.id)
        .order("created_at", { ascending: false })

      if (error) throw error
      setLoans(data || [])
    } catch (error) {
      console.error("Error fetching loans:", error)
      setError("Failed to fetch loans")
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
        .select(`
          *,
          loans (
            loan_name,
            currency
          )
        `)
        .eq("user_id", user.id)
        .order("payment_date", { ascending: false })

      if (error) throw error
      setPayments(data || [])
    } catch (error) {
      console.error("Error fetching payments:", error)
      setError("Failed to fetch payments")
    } finally {
      setLoading(false)
    }
  }

  const handleAddLoan = async (e: React.FormEvent) => {
    e.preventDefault()
    setIsAddingLoan(true)
    setError("")

    try {
      const {
        data: { user },
      } = await supabase.auth.getUser()
      if (!user) throw new Error("User not authenticated")

      const principal = Number.parseFloat(principalAmount)
      const rate = Number.parseFloat(interestRate)

      const { error } = await supabase.from("loans").insert({
        user_id: user.id,
        loan_name: loanName,
        principal_amount: principal,
        current_balance: principal,
        interest_rate: rate,
        currency: currency,
      })

      if (error) throw error

      // Reset form
      setLoanName("")
      setPrincipalAmount("")
      setInterestRate("")
      setCurrency("INR")

      await fetchLoans()
    } catch (error: any) {
      setError(error.message)
    } finally {
      setIsAddingLoan(false)
    }
  }

  const handleAddPayment = async (e: React.FormEvent) => {
    e.preventDefault()
    setIsAddingPayment(true)
    setError("")

    try {
      const {
        data: { user },
      } = await supabase.auth.getUser()
      if (!user) throw new Error("User not authenticated")

      const totalAmount = Number.parseFloat(paymentAmount)
      const principal = Number.parseFloat(principalPayment) || 0
      const interest = Number.parseFloat(interestPayment) || 0

      // Validation
      if (Math.abs(totalAmount - (principal + interest)) > 0.01) {
        throw new Error("Total amount must equal Principal + Interest")
      }

      const selectedLoan = loans.find((loan) => loan.id === selectedLoanId)
      if (!selectedLoan) throw new Error("Please select a loan")

      if (principal > selectedLoan.current_balance) {
        throw new Error("Principal payment cannot exceed current loan balance")
      }

      // Add payment record
      const { error: paymentError } = await supabase.from("loan_payments").insert({
        user_id: user.id,
        loan_id: selectedLoanId,
        amount: totalAmount,
        principal_amount: principal,
        interest_amount: interest,
        payment_date: paymentDate,
        description: paymentDescription || "Loan payment",
      })

      if (paymentError) throw paymentError

      // Update loan balance (only reduce by principal amount)
      const newBalance = selectedLoan.current_balance - principal
      const { error: updateError } = await supabase
        .from("loans")
        .update({ current_balance: Math.max(0, newBalance) })
        .eq("id", selectedLoanId)

      if (updateError) throw updateError

      // Reset form
      setSelectedLoanId("")
      setPaymentAmount("")
      setPrincipalPayment("")
      setInterestPayment("")
      setPaymentDescription("")
      setPaymentDate(new Date().toISOString().split("T")[0])

      await fetchLoans()
      await fetchPayments()
    } catch (error: any) {
      setError(error.message)
    } finally {
      setIsAddingPayment(false)
    }
  }

  const handleEditPayment = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!editingPayment) return

    setIsAddingPayment(true)
    setError("")

    try {
      const {
        data: { user },
      } = await supabase.auth.getUser()
      if (!user) throw new Error("User not authenticated")

      const totalAmount = Number.parseFloat(paymentAmount)
      const principal = Number.parseFloat(principalPayment) || 0
      const interest = Number.parseFloat(interestPayment) || 0

      // Validation
      if (Math.abs(totalAmount - (principal + interest)) > 0.01) {
        throw new Error("Total amount must equal Principal + Interest")
      }

      const selectedLoan = loans.find((loan) => loan.id === selectedLoanId)
      if (!selectedLoan) throw new Error("Please select a loan")

      // Calculate balance adjustment
      const oldPrincipal = editingPayment.principal_amount
      const newPrincipal = principal
      const principalDifference = newPrincipal - oldPrincipal
      const newBalance = selectedLoan.current_balance - principalDifference

      if (newBalance < 0) {
        throw new Error("This payment would result in a negative loan balance")
      }

      // Update payment record
      const { error: paymentError } = await supabase
        .from("loan_payments")
        .update({
          amount: totalAmount,
          principal_amount: principal,
          interest_amount: interest,
          payment_date: paymentDate,
          description: paymentDescription || "Loan payment",
        })
        .eq("id", editingPayment.id)

      if (paymentError) throw paymentError

      // Update loan balance
      const { error: updateError } = await supabase
        .from("loans")
        .update({ current_balance: Math.max(0, newBalance) })
        .eq("id", selectedLoanId)

      if (updateError) throw updateError

      setEditingPayment(null)
      await fetchLoans()
      await fetchPayments()
    } catch (error: any) {
      setError(error.message)
    } finally {
      setIsAddingPayment(false)
    }
  }

  const handleDeletePayment = async (payment: Payment) => {
    if (!confirm("Are you sure you want to delete this payment?")) return

    try {
      const {
        data: { user },
      } = await supabase.auth.getUser()
      if (!user) throw new Error("User not authenticated")

      // Delete payment record
      const { error: deleteError } = await supabase.from("loan_payments").delete().eq("id", payment.id)

      if (deleteError) throw deleteError

      // Restore loan balance (add back the principal amount)
      const loan = loans.find((l) => l.id === payment.loan_id)
      if (loan) {
        const restoredBalance = loan.current_balance + payment.principal_amount
        const { error: updateError } = await supabase
          .from("loans")
          .update({ current_balance: restoredBalance })
          .eq("id", payment.loan_id)

        if (updateError) throw updateError
      }

      await fetchLoans()
      await fetchPayments()
    } catch (error: any) {
      setError(error.message)
    }
  }

  const startEditPayment = (payment: Payment) => {
    setEditingPayment(payment)
    setSelectedLoanId(payment.loan_id)
    setPaymentAmount(payment.amount.toString())
    setPrincipalPayment(payment.principal_amount.toString())
    setInterestPayment(payment.interest_amount.toString())
    setPaymentDescription(payment.description)
    setPaymentDate(payment.payment_date)
  }

  const cancelEdit = () => {
    setEditingPayment(null)
    setSelectedLoanId("")
    setPaymentAmount("")
    setPrincipalPayment("")
    setInterestPayment("")
    setPaymentDescription("")
    setPaymentDate(new Date().toISOString().split("T")[0])
  }

  // Calculate totals
  const totalDebt = loans.reduce((sum, loan) => sum + loan.current_balance, 0)
  const totalOriginalDebt = loans.reduce((sum, loan) => sum + loan.principal_amount, 0)
  const totalInterestPaid = payments.reduce((sum, payment) => sum + payment.interest_amount, 0)

  if (loading) {
    return <div className="flex items-center justify-center p-8">Loading...</div>
  }

  return (
    <div className="space-y-6">
      {/* Summary Cards */}
      <div className="grid gap-4 md:grid-cols-3">
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Total Debt</CardTitle>
            <CreditCard className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{formatCurrency(totalDebt, "INR")}</div>
            <p className="text-xs text-muted-foreground">Current outstanding balance</p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Total Interest Paid</CardTitle>
            <TrendingDown className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold text-orange-600">{formatCurrency(totalInterestPaid, "INR")}</div>
            <p className="text-xs text-muted-foreground">Interest payments made</p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Total Paid</CardTitle>
            <DollarSign className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold text-green-600">
              {formatCurrency(totalOriginalDebt - totalDebt + totalInterestPaid, "INR")}
            </div>
            <p className="text-xs text-muted-foreground">Principal + Interest paid</p>
          </CardContent>
        </Card>
      </div>

      {error && (
        <Alert variant="destructive">
          <AlertCircle className="h-4 w-4" />
          <AlertDescription>{error}</AlertDescription>
        </Alert>
      )}

      {/* Add Loan Dialog */}
      <Dialog>
        <DialogTrigger asChild>
          <Button>
            <Plus className="mr-2 h-4 w-4" />
            Add New Loan
          </Button>
        </DialogTrigger>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Add New Loan</DialogTitle>
            <DialogDescription>Enter the details of your new loan to start tracking payments.</DialogDescription>
          </DialogHeader>
          <form onSubmit={handleAddLoan} className="space-y-4">
            <div className="space-y-2">
              <Label htmlFor="loan-name">Loan Name</Label>
              <Input
                id="loan-name"
                value={loanName}
                onChange={(e) => setLoanName(e.target.value)}
                placeholder="e.g., Home Loan, Car Loan"
                required
              />
            </div>
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label htmlFor="principal">Principal Amount</Label>
                <Input
                  id="principal"
                  type="number"
                  step="0.01"
                  value={principalAmount}
                  onChange={(e) => setPrincipalAmount(e.target.value)}
                  placeholder="100000"
                  required
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="interest-rate">Interest Rate (%)</Label>
                <Input
                  id="interest-rate"
                  type="number"
                  step="0.01"
                  value={interestRate}
                  onChange={(e) => setInterestRate(e.target.value)}
                  placeholder="8.5"
                  required
                />
              </div>
            </div>
            <div className="space-y-2">
              <Label htmlFor="currency">Currency</Label>
              <Select value={currency} onValueChange={setCurrency}>
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="INR">INR (₹)</SelectItem>
                  <SelectItem value="USD">USD ($)</SelectItem>
                </SelectContent>
              </Select>
            </div>
            <DialogFooter>
              <Button type="submit" disabled={isAddingLoan}>
                {isAddingLoan ? "Adding..." : "Add Loan"}
              </Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>

      {/* Loans List */}
      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
        {loans.map((loan) => (
          <Card key={loan.id}>
            <CardHeader>
              <CardTitle className="text-lg">{loan.loan_name}</CardTitle>
              <CardDescription>{loan.interest_rate}% interest rate</CardDescription>
            </CardHeader>
            <CardContent>
              <div className="space-y-2">
                <div className="flex justify-between">
                  <span className="text-sm text-muted-foreground">Original:</span>
                  <span className="font-medium">{formatCurrency(loan.principal_amount, loan.currency)}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-sm text-muted-foreground">Current:</span>
                  <span className="font-bold text-lg">{formatCurrency(loan.current_balance, loan.currency)}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-sm text-muted-foreground">Paid:</span>
                  <span className="font-medium text-green-600">
                    {formatCurrency(loan.principal_amount - loan.current_balance, loan.currency)}
                  </span>
                </div>
              </div>
            </CardContent>
          </Card>
        ))}
      </div>

      {/* Add Payment Dialog */}
      <Dialog open={!!editingPayment || false}>
        <DialogTrigger asChild>
          <Button variant="outline" disabled={loans.length === 0}>
            <Plus className="mr-2 h-4 w-4" />
            Record Payment
          </Button>
        </DialogTrigger>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>{editingPayment ? "Edit Payment" : "Record Loan Payment"}</DialogTitle>
            <DialogDescription>Record a payment made towards one of your loans.</DialogDescription>
          </DialogHeader>

          <Alert>
            <Info className="h-4 w-4" />
            <AlertDescription>
              <strong>Principal Amount:</strong> Reduces your loan balance
              <br />
              <strong>Interest Amount:</strong> Tracked separately, doesn't reduce balance
            </AlertDescription>
          </Alert>

          <form onSubmit={editingPayment ? handleEditPayment : handleAddPayment} className="space-y-4">
            <div className="space-y-2">
              <Label htmlFor="loan-select">Select Loan</Label>
              <Select value={selectedLoanId} onValueChange={setSelectedLoanId} required>
                <SelectTrigger>
                  <SelectValue placeholder="Choose a loan" />
                </SelectTrigger>
                <SelectContent>
                  {loans.map((loan) => (
                    <SelectItem key={loan.id} value={loan.id}>
                      {loan.loan_name} - {formatCurrency(loan.current_balance, loan.currency)}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label htmlFor="principal-payment">
                  Principal Amount
                  <Badge variant="secondary" className="ml-2 text-xs">
                    Reduces Balance
                  </Badge>
                </Label>
                <Input
                  id="principal-payment"
                  type="number"
                  step="0.01"
                  value={principalPayment}
                  onChange={(e) => setPrincipalPayment(e.target.value)}
                  placeholder="0.00"
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="interest-payment">
                  Interest Amount
                  <Badge variant="outline" className="ml-2 text-xs text-orange-600">
                    Interest Only
                  </Badge>
                </Label>
                <Input
                  id="interest-payment"
                  type="number"
                  step="0.01"
                  value={interestPayment}
                  onChange={(e) => setInterestPayment(e.target.value)}
                  placeholder="0.00"
                />
              </div>
            </div>

            <div className="space-y-2">
              <Label htmlFor="total-payment">Total Payment Amount</Label>
              <Input
                id="total-payment"
                type="number"
                step="0.01"
                value={paymentAmount}
                onChange={(e) => setPaymentAmount(e.target.value)}
                placeholder="0.00"
                required
                readOnly
                className="bg-muted"
              />
              <p className="text-xs text-muted-foreground">Auto-calculated from Principal + Interest</p>
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label htmlFor="payment-date">Payment Date</Label>
                <Input
                  id="payment-date"
                  type="date"
                  value={paymentDate}
                  onChange={(e) => setPaymentDate(e.target.value)}
                  required
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="description">Description</Label>
                <Input
                  id="description"
                  value={paymentDescription}
                  onChange={(e) => setPaymentDescription(e.target.value)}
                  placeholder="Monthly payment"
                />
              </div>
            </div>

            <DialogFooter>
              {editingPayment && (
                <Button type="button" variant="outline" onClick={cancelEdit}>
                  Cancel
                </Button>
              )}
              <Button type="submit" disabled={isAddingPayment}>
                {isAddingPayment ? "Processing..." : editingPayment ? "Update Payment" : "Record Payment"}
              </Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>

      {/* Payment History */}
      <Card>
        <CardHeader>
          <CardTitle>Payment History</CardTitle>
          <CardDescription>Recent loan payments and their breakdown</CardDescription>
        </CardHeader>
        <CardContent>
          {payments.length === 0 ? (
            <p className="text-center text-muted-foreground py-8">
              No payments recorded yet. Add a payment to get started.
            </p>
          ) : (
            <div className="space-y-4">
              {payments.map((payment) => (
                <div key={payment.id} className="flex items-center justify-between p-4 border rounded-lg">
                  <div className="space-y-1">
                    <div className="flex items-center gap-2">
                      <span className="font-medium">{payment.loans.loan_name}</span>
                      <Badge variant="outline">{payment.payment_date}</Badge>
                    </div>
                    <div className="text-sm text-muted-foreground">{payment.description}</div>
                    <div className="flex gap-4 text-sm">
                      <span>
                        Principal:{" "}
                        <span className="font-medium text-green-600">
                          {formatCurrency(payment.principal_amount, payment.loans.currency)}
                        </span>
                      </span>
                      {payment.interest_amount > 0 && (
                        <span>
                          Interest:{" "}
                          <span className="font-medium text-orange-600">
                            {formatCurrency(payment.interest_amount, payment.loans.currency)}
                          </span>
                        </span>
                      )}
                    </div>
                  </div>
                  <div className="flex items-center gap-2">
                    <div className="text-right">
                      <div className="font-bold">{formatCurrency(payment.amount, payment.loans.currency)}</div>
                    </div>
                    <div className="flex gap-1">
                      <Button size="sm" variant="ghost" onClick={() => startEditPayment(payment)}>
                        <Edit className="h-4 w-4" />
                      </Button>
                      <Button size="sm" variant="ghost" onClick={() => handleDeletePayment(payment)}>
                        <Trash2 className="h-4 w-4" />
                      </Button>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  )
}
