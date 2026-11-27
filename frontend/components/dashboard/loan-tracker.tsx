"use client"

import type React from "react"

import { useEffect, useState } from "react"
import { useAuth } from "@/components/auth/auth-provider"
import { API, apiGetPage, apiRequest } from "@/lib/api"
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
import { formatINR } from "@/lib/utils"

interface Loan {
  id: string
  loanName: string
  principalAmount: number
  currentBalance: number
  interestRate: number | null
  monthlyPayment: number | null
  startDate: string
  endDate: string | null
  lender: string | null
  loanType: string | null
  currency: "INR" | "USD"
  status: string
}

interface Payment {
  id: string
  loanId: string
  amount: number
  principalAmount: number
  interestAmount: number
  paymentDate: string
  paymentMethod?: string | null
  currency: string
  notes?: string
  loan?: Loan
}

type LoanPaymentWithLoan = { interestAmount: number; currency: string }

export default function LoanTracker() {
  const { accessToken } = useAuth()
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
    if (!accessToken) {
      setError("Please sign in to view loans.")
      setLoading(false)
      return
    }
    try {
      setError("")
      const loanPage = await apiGetPage<Loan>(`${API.loans}?page=0&size=1000&sort=createdAt,desc`, accessToken)
      const loansData = loanPage.content
      // Payments are a nested resource because the backend verifies ownership
      // of both the loan and each payment before returning or mutating records.
      const paymentPages = await Promise.all(
        loansData.map(async (loan) => {
          const page = await apiGetPage<Payment>(
            `${API.loans}/${loan.id}/payments?page=0&size=1000&sort=paymentDate,desc`,
            accessToken
          )
          return page.content.map((payment) => ({ ...payment, loan }))
        })
      )
      const paymentsData = paymentPages.flat()
      setLoans(loansData)
      setPayments(paymentsData)
      const totalInterest = paymentsData.reduce((sum, payment) => {
        const interestInINR = payment.currency === "USD"
          ? payment.interestAmount * 83.25
          : payment.interestAmount
        return sum + interestInINR
      }, 0)
      setTotalInterestPaid(totalInterest)
    } catch (error) {
      console.error("Error fetching loan data:", error)
      setError(error instanceof Error ? error.message : "Failed to fetch loans and payments")
    } finally {
      setLoading(false)
    }
  }

  const handleAddLoan = async (e: React.FormEvent) => {
    e.preventDefault()
    setLoading(true)
    setError("")
    if (!accessToken) {
      setError("Your session has expired. Please sign in again.")
      setLoading(false)
      return
    }
    try {
      const principalAmount = Number.parseFloat(loanForm.principal_amount)
      const payload = {
        loanName: loanForm.loan_name,
        principalAmount,
        currentBalance: principalAmount,
        interestRate: Number.parseFloat(loanForm.interest_rate) || 0,
        monthlyPayment: null,
        startDate: new Date().toISOString().slice(0, 10),
        endDate: null,
        lender: null,
        loanType: "personal",
        currency: loanForm.currency,
        status: "active",
      }
      await apiRequest<Loan>(API.loans, accessToken, {
        method: "POST", body: JSON.stringify(payload),
      })
      setSuccess("Loan added successfully!")
      setIsAddingLoan(false)
      setLoanForm({ loan_name: "", principal_amount: "", interest_rate: "", currency: "INR" })
      await fetchData()
    } catch (error) {
      setError(error instanceof Error ? error.message : "Failed to add loan")
    } finally {
      setLoading(false)
    }
  }

  const handleAddPayment = async (e: React.FormEvent) => {
    e.preventDefault()
    setLoading(true)
    setError("")
    if (!accessToken) {
      setError("Your session has expired. Please sign in again.")
      setLoading(false)
      return
    }
    try {
      const principalAmount = Number.parseFloat(paymentForm.principal_amount) || 0
      const interestAmount = Number.parseFloat(paymentForm.interest_amount) || 0
      const totalAmount = principalAmount + interestAmount
      if (totalAmount <= 0 || Number.parseFloat(paymentForm.total_amount) !== totalAmount) {
        setError("Payment amount must equal principal + interest and be greater than zero.")
        return
      }
      const loan = loans.find((item) => item.id === paymentForm.loan_id)
      if (!loan) {
        setError("Select a valid loan before recording a payment.")
        return
      }
      const payload = {
        amount: totalAmount,
        principalAmount,
        interestAmount,
        paymentDate: paymentForm.payment_date,
        paymentMethod: null,
        notes: paymentForm.notes || null,
        currency: loan.currency,
        exchangeRate: null,
        inrEquivalent: loan.currency === "INR" ? totalAmount : null,
      }
      await apiRequest<Payment>(`${API.loans}/${loan.id}/payments`, accessToken, {
        method: "POST", body: JSON.stringify(payload),
      })
      setSuccess("Payment recorded successfully!")
      setIsAddingPayment(false)
      setPaymentForm({
        loan_id: "", total_amount: "", principal_amount: "", interest_amount: "",
        payment_date: new Date().toISOString().split("T")[0], notes: "",
      })
      await fetchData()
    } catch (error) {
      setError(error instanceof Error ? error.message : "Failed to record payment")
    } finally {
      setLoading(false)
    }
  }

  const handleEditPayment = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!editingPayment || !accessToken) return
    setLoading(true)
    setError("")
    try {
      const principalAmount = Number.parseFloat(paymentForm.principal_amount) || 0
      const interestAmount = Number.parseFloat(paymentForm.interest_amount) || 0
      const totalAmount = principalAmount + interestAmount
      if (totalAmount <= 0) {
        setError("Payment amount must be greater than zero.")
        return
      }
      const loan = loans.find((item) => item.id === editingPayment.loanId)
      if (!loan) {
        setError("The parent loan for this payment could not be found.")
        return
      }
      const payload = {
        amount: totalAmount,
        principalAmount,
        interestAmount,
        paymentDate: paymentForm.payment_date,
        paymentMethod: editingPayment.paymentMethod ?? null,
        notes: paymentForm.notes || null,
        currency: loan.currency,
        exchangeRate: null,
        inrEquivalent: loan.currency === "INR" ? totalAmount : null,
      }
      await apiRequest<Payment>(
        `${API.loans}/${loan.id}/payments/${editingPayment.id}`,
        accessToken,
        { method: "PUT", body: JSON.stringify(payload) }
      )
      setSuccess("Payment updated successfully!")
      setEditingPayment(null)
      setPaymentForm({
        loan_id: "", total_amount: "", principal_amount: "", interest_amount: "",
        payment_date: new Date().toISOString().split("T")[0], notes: "",
      })
      await fetchData()
    } catch (error) {
      setError(error instanceof Error ? error.message : "Failed to update payment")
    } finally {
      setLoading(false)
    }
  }

  const handleDeletePayment = async (payment: Payment) => {
    if (!confirm("Are you sure you want to delete this payment?")) return
    if (!accessToken) {
      setError("Your session has expired. Please sign in again.")
      return
    }
    setLoading(true)
    setError("")
    try {
      // The backend reverses the principal reduction in the same transaction.
      await apiRequest<void>(
        `${API.loans}/${payment.loanId}/payments/${payment.id}`,
        accessToken,
        { method: "DELETE" }
      )
      setSuccess("Payment deleted successfully!")
      await fetchData()
    } catch (error) {
      setError(error instanceof Error ? error.message : "Failed to delete payment")
    } finally {
      setLoading(false)
    }
  }

  const startEditPayment = (payment: Payment) => {
    setEditingPayment(payment)
    setPaymentForm({
      loan_id: payment.loanId,
      total_amount: payment.amount.toString(),
      principal_amount: payment.principalAmount.toString(),
      interest_amount: payment.interestAmount.toString(),
      payment_date: payment.paymentDate,
      notes: payment.notes || "",
    })
  }

  const getTotalDebt = (): number => {
    return loans.reduce((sum, loan) => {
      const balanceInINR =
        loan.currency === "USD"
          ? loan.currentBalance * 83.25
          : loan.currentBalance
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
                  and doesn&apos;t reduce the balance.
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
                            {loan.loanName} - {loan.currency === "INR" ? "₹" : "$"}
                            {loan.currentBalance.toLocaleString()}
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
                      <h3 className="font-medium">{loan.loanName}</h3>
                      <div className="flex items-center gap-2 text-sm text-muted-foreground">
                        <span>
                          {loan.currency === "INR" ? "₹" : "$"}
                          {loan.currentBalance.toLocaleString()} remaining
                        </span>
                        <Badge variant={loan.status === "active" ? "default" : "secondary"}>{loan.status}</Badge>
                      </div>
                    </div>
                  </div>
                  <div className="text-right">
                    <div className="text-sm font-medium">{loan.interestRate}% interest</div>
                    <div className="text-xs text-muted-foreground">
                      Principal: {loan.currency === "INR" ? "₹" : "$"}
                      {loan.principalAmount.toLocaleString()}
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
                    <TableCell className="font-medium">{payment.loan?.loanName || "Unknown Loan"}</TableCell>
                    <TableCell>{new Date(payment.paymentDate).toLocaleDateString()}</TableCell>
                    <TableCell>
                      <div className="flex items-center gap-1">
                        {payment.loan?.currency === "INR" ? (
                          <IndianRupee className="h-3 w-3" />
                        ) : (
                          <DollarSign className="h-3 w-3" />
                        )}
                        {payment.principalAmount.toLocaleString()}
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
                        {payment.interestAmount.toLocaleString()}
                        {payment.interestAmount > 0 && (
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
