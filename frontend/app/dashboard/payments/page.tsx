"use client"

export const dynamic = "force-dynamic"

import type React from "react"

import { useEffect, useState } from "react"
import { useAuth } from "@/components/auth/auth-provider"
import { API, apiGetPage, apiRequest } from "@/lib/api"
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
import { Calendar } from "@/components/ui/calendar"
import { Popover, PopoverContent, PopoverTrigger } from "@/components/ui/popover"
import { Plus, Edit, Trash2, CalendarIcon, Clock, Bell, CheckCircle } from "lucide-react"
import { formatCurrency, formatDate, toUSD, formatUSDDisplay } from "@/lib/utils"
import { cn } from "@/lib/utils"
import { format } from "date-fns"

interface ScheduledPayment {
  id: string
  paymentName: string
  amount: number
  currency: string
  category: string | null
  recipient: string | null
  dueDate: string
  frequency: string | null
  recurring: boolean
  autoPay: boolean
  status: string
  reminderDays: number
  notes: string | null
}

const paymentCategories = [
  "Utilities",
  "Rent/Mortgage",
  "Insurance",
  "Loan Payment",
  "Subscription",
  "Credit Card",
  "Tax Payment",
  "Other",
]

const frequencies = [
  { value: "once", label: "One-time" },
  { value: "weekly", label: "Weekly" },
  { value: "monthly", label: "Monthly" },
  { value: "quarterly", label: "Quarterly" },
  { value: "yearly", label: "Yearly" },
]

export default function ScheduledPaymentsPage() {
  const { accessToken } = useAuth()
  const [payments, setPayments] = useState<ScheduledPayment[]>([])
  const [loading, setLoading] = useState(true)
  const [dialogOpen, setDialogOpen] = useState(false)
  const [editingPayment, setEditingPayment] = useState<ScheduledPayment | null>(null)
  const [selectedDate, setSelectedDate] = useState<Date>()

  const [formData, setFormData] = useState({
    paymentName: "",
    amount: "",
    currency: "USD",
    category: "",
    recipient: "",
    dueDate: new Date().toISOString().split("T")[0],
    frequency: "once",
    recurring: false,
    autoPay: false,
    reminderDays: 3,
    notes: "",
  })

  useEffect(() => {
    fetchPayments()
  }, [])

  const fetchPayments = async () => {
    if (!accessToken) { setLoading(false); return }
    try {
      const page = await apiGetPage<ScheduledPayment>(
        `${API.scheduledPayments}?page=0&size=1000&sort=dueDate,asc`, accessToken
      )
      setPayments(page.content)
    } catch (error) {
      console.error("Error fetching scheduled payments:", error)
    } finally { setLoading(false) }
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!accessToken) return
    try {
      const paymentData = {
        paymentName: formData.payment_name,
        amount: Number.parseFloat(formData.amount),
        currency: formData.currency,
        category: formData.category || null,
        recipient: formData.recipient || null,
        dueDate: formData.due_date,
        frequency: formData.frequency,
        recurring: formData.is_recurring,
        autoPay: formData.auto_pay,
        status: editingPayment?.status ?? "pending",
        reminderDays: formData.reminder_days,
        notes: formData.notes || null,
      }
      if (editingPayment) {
        await apiRequest<ScheduledPayment>(`${API.scheduledPayments}/${editingPayment.id}`, accessToken, {
          method: "PUT", body: JSON.stringify(paymentData),
        })
      } else {
        await apiRequest<ScheduledPayment>(API.scheduledPayments, accessToken, {
          method: "POST", body: JSON.stringify(paymentData),
        })
      }
      await fetchPayments()
      setDialogOpen(false)
      resetForm()
    } catch (error) {
      console.error("Error saving scheduled payment:", error)
    }
  }

  const handleEdit = (payment: ScheduledPayment) => {
    setEditingPayment(payment)
    setFormData({
      paymentName: payment.paymentName,
      amount: payment.amount.toString(),
      currency: payment.currency,
      category: payment.category || "",
      recipient: payment.recipient || "",
      dueDate: payment.dueDate,
      frequency: payment.frequency || "once",
      recurring: payment.recurring,
      autoPay: payment.autoPay,
      reminderDays: payment.reminderDays,
      notes: payment.notes || "",
    })
    setDialogOpen(true)
  }

  const handleDelete = async (id: string) => {
    if (!accessToken) return
    try {
      await apiRequest<void>(`${API.scheduledPayments}/${id}`, accessToken, { method: "DELETE" })
      await fetchPayments()
    } catch (error) {
      console.error("Error deleting scheduled payment:", error)
    }
  }

  const handleMarkPaid = async (id: string) => {
    if (!accessToken) return
    const payment = payments.find((item) => item.id === id)
    if (!payment) return
    try {
      // PUT is a full replacement in the backend contract; preserve all fields
      // so changing the status cannot accidentally clear the user's reminder data.
      const payload = {
        paymentName: payment.paymentName,
        amount: payment.amount,
        currency: payment.currency,
        category: payment.category,
        recipient: payment.recipient,
        dueDate: payment.dueDate,
        frequency: payment.frequency,
        recurring: payment.recurring,
        autoPay: payment.autoPay,
        status: "paid",
        reminderDays: payment.reminderDays,
        notes: payment.notes,
      }
      await apiRequest<ScheduledPayment>(`${API.scheduledPayments}/${id}`, accessToken, {
        method: "PUT", body: JSON.stringify(payload),
      })
      await fetchPayments()
    } catch (error) {
      console.error("Error marking scheduled payment as paid:", error)
    }
  }

  const resetForm = () => {
    setFormData({
      paymentName: "",
      amount: "",
      currency: "USD",
      category: "",
      recipient: "",
      dueDate: new Date().toISOString().split("T")[0],
      frequency: "once",
      recurring: false,
      autoPay: false,
      reminderDays: 3,
      notes: "",
    })
    setEditingPayment(null)
  }

  const upcomingPayments = payments.filter((p) => p.status === "pending")
  const totalUpcoming = upcomingPayments.reduce((sum, payment) => sum + payment.amount, 0)
  const overduePayments = payments.filter((p) => p.status === "pending" && new Date(p.dueDate) < new Date()).length

  if (loading) {
    return (
      <div className="space-y-6">
        <div>
          <h1 className="text-3xl font-bold tracking-tight">Scheduled Payments</h1>
          <p className="text-muted-foreground">Manage your upcoming payments and reminders</p>
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
        <h1 className="text-3xl font-bold tracking-tight">Scheduled Payments</h1>
        <p className="text-muted-foreground">Manage your upcoming payments and reminders</p>
      </div>

      {/* Summary Cards */}
      <div className="grid gap-4 md:grid-cols-4">
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Upcoming Total</CardTitle>
            <CalendarIcon className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold text-orange-600">{formatUSDDisplay(toUSD(totalUpcoming, "USD"))}</div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Pending Payments</CardTitle>
            <Clock className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{upcomingPayments.length}</div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Overdue</CardTitle>
            <Bell className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold text-red-600">{overduePayments}</div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Auto Pay</CardTitle>
            <CheckCircle className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold text-green-600">{payments.filter((p) => p.auto_pay).length}</div>
          </CardContent>
        </Card>
      </div>

      {/* Payments Management */}
      <Card>
        <CardHeader>
          <div className="flex items-center justify-between">
            <div>
              <CardTitle>Scheduled Payments</CardTitle>
              <CardDescription>Manage your upcoming payments and set reminders</CardDescription>
            </div>
            <Dialog open={dialogOpen} onOpenChange={setDialogOpen}>
              <DialogTrigger asChild>
                <Button onClick={resetForm}>
                  <Plus className="mr-2 h-4 w-4" />
                  Schedule Payment
                </Button>
              </DialogTrigger>
              <DialogContent className="sm:max-w-[500px]">
                <DialogHeader>
                  <DialogTitle>{editingPayment ? "Edit Payment" : "Schedule New Payment"}</DialogTitle>
                  <DialogDescription>
                    {editingPayment ? "Update payment details" : "Schedule a new payment with reminders"}
                  </DialogDescription>
                </DialogHeader>
                <form onSubmit={handleSubmit} className="space-y-4">
                  <div className="space-y-2">
                    <Label htmlFor="payment_name">Payment Name</Label>
                    <Input
                      id="payment_name"
                      placeholder="e.g., Electric Bill, Rent"
                      value={formData.payment_name}
                      onChange={(e) => setFormData({ ...formData, payment_name: e.target.value })}
                      required
                    />
                  </div>

                  <div className="grid grid-cols-2 gap-4">
                    <div className="space-y-2">
                      <Label htmlFor="amount">Amount</Label>
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
                      <Label htmlFor="currency">Currency</Label>
                      <Select
                        value={formData.currency}
                        onValueChange={(value) => setFormData({ ...formData, currency: value })}
                      >
                        <SelectTrigger>
                          <SelectValue />
                        </SelectTrigger>
                        <SelectContent>
                          <SelectItem value="USD">USD</SelectItem>
                          <SelectItem value="EUR">EUR</SelectItem>
                          <SelectItem value="GBP">GBP</SelectItem>
                          <SelectItem value="CAD">CAD</SelectItem>
                          <SelectItem value="AUD">AUD</SelectItem>
                          <SelectItem value="INR">INR</SelectItem>
                        </SelectContent>
                      </Select>
                    </div>
                  </div>

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
                          {paymentCategories.map((category) => (
                            <SelectItem key={category} value={category.toLowerCase()}>
                              {category}
                            </SelectItem>
                          ))}
                        </SelectContent>
                      </Select>
                    </div>
                    <div className="space-y-2">
                      <Label htmlFor="recipient">Recipient</Label>
                      <Input
                        id="recipient"
                        placeholder="e.g., Electric Company"
                        value={formData.recipient}
                        onChange={(e) => setFormData({ ...formData, recipient: e.target.value })}
                      />
                    </div>
                  </div>

                  <div className="space-y-2">
                    <Label>Due Date</Label>
                    <Popover>
                      <PopoverTrigger asChild>
                        <Button
                          variant="outline"
                          className={cn(
                            "w-full justify-start text-left font-normal",
                            !formData.due_date && "text-muted-foreground",
                          )}
                        >
                          <CalendarIcon className="mr-2 h-4 w-4" />
                          {formData.due_date ? format(new Date(formData.due_date), "PPP") : "Pick a date"}
                        </Button>
                      </PopoverTrigger>
                      <PopoverContent className="w-auto p-0">
                        <Calendar
                          mode="single"
                          selected={formData.due_date ? new Date(formData.due_date) : undefined}
                          onSelect={(date) =>
                            setFormData({ ...formData, due_date: date?.toISOString().split("T")[0] || "" })
                          }
                          initialFocus
                        />
                      </PopoverContent>
                    </Popover>
                  </div>

                  <div className="flex items-center space-x-2">
                    <Switch
                      id="recurring"
                      checked={formData.is_recurring}
                      onCheckedChange={(checked) => setFormData({ ...formData, is_recurring: checked })}
                    />
                    <Label htmlFor="recurring">Recurring Payment</Label>
                  </div>

                  {formData.is_recurring && (
                    <div className="space-y-2">
                      <Label htmlFor="frequency">Frequency</Label>
                      <Select
                        value={formData.frequency}
                        onValueChange={(value) => setFormData({ ...formData, frequency: value })}
                      >
                        <SelectTrigger>
                          <SelectValue />
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

                  <div className="flex items-center space-x-2">
                    <Switch
                      id="auto_pay"
                      checked={formData.auto_pay}
                      onCheckedChange={(checked) => setFormData({ ...formData, auto_pay: checked })}
                    />
                    <Label htmlFor="auto_pay">Auto Pay (when available)</Label>
                  </div>

                  <div className="space-y-2">
                    <Label htmlFor="reminder_days">Reminder Days Before</Label>
                    <Select
                      value={formData.reminder_days.toString()}
                      onValueChange={(value) => setFormData({ ...formData, reminder_days: Number.parseInt(value) })}
                    >
                      <SelectTrigger>
                        <SelectValue />
                      </SelectTrigger>
                      <SelectContent>
                        <SelectItem value="1">1 day</SelectItem>
                        <SelectItem value="3">3 days</SelectItem>
                        <SelectItem value="7">1 week</SelectItem>
                        <SelectItem value="14">2 weeks</SelectItem>
                      </SelectContent>
                    </Select>
                  </div>

                  <div className="space-y-2">
                    <Label htmlFor="notes">Notes (Optional)</Label>
                    <Textarea
                      id="notes"
                      placeholder="Additional notes about this payment"
                      value={formData.notes}
                      onChange={(e) => setFormData({ ...formData, notes: e.target.value })}
                    />
                  </div>

                  <div className="flex justify-end space-x-2">
                    <Button type="button" variant="outline" onClick={() => setDialogOpen(false)}>
                      Cancel
                    </Button>
                    <Button type="submit">{editingPayment ? "Update" : "Schedule"} Payment</Button>
                  </div>
                </form>
              </DialogContent>
            </Dialog>
          </div>
        </CardHeader>
        <CardContent>
          {payments.length === 0 ? (
            <div className="text-center py-8">
              <CalendarIcon className="mx-auto h-12 w-12 text-muted-foreground" />
              <h3 className="mt-2 text-sm font-semibold text-foreground">No payments scheduled</h3>
              <p className="mt-1 text-sm text-muted-foreground">Get started by scheduling your first payment.</p>
            </div>
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Payment</TableHead>
                  <TableHead>Recipient</TableHead>
                  <TableHead>Amount</TableHead>
                  <TableHead>Due Date</TableHead>
                  <TableHead>Status</TableHead>
                  <TableHead>Type</TableHead>
                  <TableHead className="text-right">Actions</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {payments.map((payment) => {
                  const isOverdue = new Date(payment.due_date) < new Date() && payment.status === "pending"
                  const isDueSoon =
                    new Date(payment.due_date) <= new Date(Date.now() + payment.reminder_days * 24 * 60 * 60 * 1000) &&
                    payment.status === "pending"

                  return (
                    <TableRow key={payment.id} className={isOverdue ? "bg-red-50 dark:bg-red-950/30" : isDueSoon ? "bg-yellow-50 dark:bg-yellow-950/30" : ""}>
                      <TableCell>
                        <div>
                          <p className="font-medium">{payment.payment_name}</p>
                          {payment.category && <p className="text-sm text-muted-foreground">{payment.category}</p>}
                        </div>
                      </TableCell>
                      <TableCell>{payment.recipient || "—"}</TableCell>
                      <TableCell className="font-semibold">
                        {formatUSDDisplay(toUSD(payment.amount, payment.currency))}
                      </TableCell>
                      <TableCell>
                        <div>
                          <p>{formatDate(payment.due_date)}</p>
                          {isOverdue && <p className="text-xs text-red-600">Overdue</p>}
                          {isDueSoon && !isOverdue && <p className="text-xs text-yellow-600">Due soon</p>}
                        </div>
                      </TableCell>
                      <TableCell>
                        <Badge
                          variant={
                            payment.status === "paid"
                              ? "default"
                              : isOverdue
                                ? "destructive"
                                : isDueSoon
                                  ? "secondary"
                                  : "outline"
                          }
                        >
                          {payment.status === "paid" ? "Paid" : isOverdue ? "Overdue" : "Pending"}
                        </Badge>
                      </TableCell>
                      <TableCell>
                        <div className="flex items-center gap-1">
                          {payment.is_recurring && <Badge variant="outline">Recurring</Badge>}
                          {payment.auto_pay && <Badge variant="secondary">Auto Pay</Badge>}
                        </div>
                      </TableCell>
                      <TableCell className="text-right">
                        <div className="flex justify-end space-x-2">
                          {payment.status === "pending" && (
                            <Button variant="ghost" size="sm" onClick={() => handleMarkPaid(payment.id)}>
                              <CheckCircle className="h-4 w-4" />
                            </Button>
                          )}
                          <Button variant="ghost" size="sm" onClick={() => handleEdit(payment)}>
                            <Edit className="h-4 w-4" />
                          </Button>
                          <Button variant="ghost" size="sm" onClick={() => handleDelete(payment.id)}>
                            <Trash2 className="h-4 w-4" />
                          </Button>
                        </div>
                      </TableCell>
                    </TableRow>
                  )
                })}
              </TableBody>
            </Table>
          )}
        </CardContent>
      </Card>
    </div>
  )
}
