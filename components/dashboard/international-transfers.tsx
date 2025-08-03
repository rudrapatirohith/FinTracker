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
import { Badge } from "@/components/ui/badge"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { Alert, AlertDescription } from "@/components/ui/alert"
import {
  Plus,
  Edit,
  Trash2,
  Send,
  Globe,
  TrendingUp,
  RefreshCw,
  AlertCircle,
  DollarSign,
  IndianRupee,
} from "lucide-react"
import { formatUSD, formatINR, formatTransferCurrency, convertUSDToINR } from "@/lib/utils"

interface Transfer {
  id: string
  recipient_name: string
  recipient_country: string
  amount_sent: number
  currency_sent: string
  amount_received: number
  currency_received: string
  exchange_rate: number
  transfer_fee: number
  transfer_method: string | null
  purpose: string | null
  status: string
  transfer_date: string
  completion_date: string | null
  reference_number: string | null
}

// Countries with focus on India
const countries = [
  { code: "IN", name: "India", currency: "INR", flag: "🇮🇳" },
  { code: "IT", name: "Italy", currency: "EUR", flag: "🇮🇹" },
  { code: "CA", name: "Canada", currency: "CAD", flag: "🇨🇦" },
  { code: "GB", name: "United Kingdom", currency: "GBP", flag: "🇬🇧" },
  { code: "AU", name: "Australia", currency: "AUD", flag: "🇦🇺" },
]

const transferMethods = [
  "Wire Transfer",
  "Remittance Service (Wise, Remitly)",
  "Digital Wallet (PayPal, Skrill)",
  "Bank Transfer",
  "Money Order",
  "Cryptocurrency",
  "Other",
]

const transferPurposes = [
  "Family Support",
  "Education Fees",
  "Medical Expenses",
  "Property Investment",
  "Business Investment",
  "Personal Savings",
  "Gift",
  "Other",
]

export default function InternationalTransfers() {
  const [transfers, setTransfers] = useState<Transfer[]>([])
  const [loading, setLoading] = useState(true)
  const [dialogOpen, setDialogOpen] = useState(false)
  const [editingTransfer, setEditingTransfer] = useState<Transfer | null>(null)
  const [exchangeRates, setExchangeRates] = useState<Record<string, number>>({})
  const [loadingRates, setLoadingRates] = useState(false)
  const [error, setError] = useState("")
  const [success, setSuccess] = useState("")

  const [formData, setFormData] = useState({
    recipient_name: "",
    recipient_country: "India",
    amount_sent: "",
    currency_sent: "USD",
    amount_received: "",
    currency_received: "INR",
    exchange_rate: "",
    transfer_fee: "",
    transfer_method: "",
    purpose: "",
    status: "pending",
    transfer_date: new Date().toISOString().split("T")[0],
    reference_number: "",
  })

  useEffect(() => {
    fetchTransfers()
    fetchExchangeRates()
  }, [])

  const fetchTransfers = async () => {
    try {
      setError("")
      const {
        data: { user },
      } = await supabase.auth.getUser()

      if (!user) {
        setError("Please log in to view transfers")
        return
      }

      console.log("Fetching transfers for user:", user.id)

      const { data, error } = await supabase
        .from("international_transfers")
        .select("*")
        .eq("user_id", user.id)
        .order("transfer_date", { ascending: false })

      if (error) {
        handleSupabaseError(error, "fetch transfers")
        return
      }

      console.log("Fetched transfers:", data)
      setTransfers(data || [])
    } catch (error: any) {
      console.error("Error fetching transfers:", error)
      setError(error.message || "Failed to fetch transfers")
    } finally {
      setLoading(false)
    }
  }

  const fetchExchangeRates = async () => {
    setLoadingRates(true)
    try {
      // Using a free exchange rate API for USD to various currencies
      const response = await fetch("https://api.exchangerate-api.com/v4/latest/USD")
      const data = await response.json()

      // Focus on currencies we support
      const filteredRates = {
        INR: data.rates.INR || 83.12,
        EUR: data.rates.EUR || 0.85,
        CAD: data.rates.CAD || 1.25,
        GBP: data.rates.GBP || 0.79,
        AUD: data.rates.AUD || 1.35,
      }

      setExchangeRates(filteredRates)
    } catch (error) {
      console.error("Error fetching exchange rates:", error)
      // Fallback rates
      setExchangeRates({
        INR: 83.12, // India - Primary focus
        EUR: 0.85, // Italy
        CAD: 1.25, // Canada
        GBP: 0.79, // London/UK
        AUD: 1.35, // Australia
      })
    } finally {
      setLoadingRates(false)
    }
  }

  const calculateReceived = (sent: string, rate: string) => {
    const sentAmount = Number.parseFloat(sent)
    const exchangeRate = Number.parseFloat(rate)
    if (sentAmount && exchangeRate) {
      return (sentAmount * exchangeRate).toFixed(2)
    }
    return ""
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
        setError("Please log in to add transfers")
        return
      }

      console.log("Submitting transfer data...")

      const transferData = {
        user_id: user.id,
        recipient_name: formData.recipient_name,
        recipient_country: formData.recipient_country,
        amount_sent: Number.parseFloat(formData.amount_sent),
        currency_sent: formData.currency_sent,
        amount_received: Number.parseFloat(formData.amount_received),
        currency_received: formData.currency_received,
        exchange_rate: Number.parseFloat(formData.exchange_rate),
        transfer_fee: Number.parseFloat(formData.transfer_fee) || 0,
        transfer_method: formData.transfer_method || null,
        purpose: formData.purpose || null,
        status: formData.status,
        transfer_date: formData.transfer_date,
        reference_number: formData.reference_number || null,
      }

      console.log("Transfer data to submit:", transferData)

      let result
      if (editingTransfer) {
        result = await supabase
          .from("international_transfers")
          .update(transferData)
          .eq("id", editingTransfer.id)
          .select()
      } else {
        result = await supabase.from("international_transfers").insert([transferData]).select()
      }

      if (result.error) {
        handleSupabaseError(result.error, editingTransfer ? "update transfer" : "add transfer")
        return
      }

      console.log("Transfer saved successfully:", result.data)
      setSuccess(editingTransfer ? "Transfer updated successfully!" : "Transfer added successfully!")

      await fetchTransfers()
      setDialogOpen(false)
      resetForm()

      setTimeout(() => setSuccess(""), 3000)
    } catch (error: any) {
      console.error("Error saving transfer:", error)
      setError(error.message || "Failed to save transfer")
    }
  }

  const handleEdit = (transfer: Transfer) => {
    setEditingTransfer(transfer)
    setFormData({
      recipient_name: transfer.recipient_name,
      recipient_country: transfer.recipient_country,
      amount_sent: transfer.amount_sent.toString(),
      currency_sent: transfer.currency_sent,
      amount_received: transfer.amount_received.toString(),
      currency_received: transfer.currency_received,
      exchange_rate: transfer.exchange_rate.toString(),
      transfer_fee: transfer.transfer_fee.toString(),
      transfer_method: transfer.transfer_method || "",
      purpose: transfer.purpose || "",
      status: transfer.status,
      transfer_date: transfer.transfer_date,
      reference_number: transfer.reference_number || "",
    })
    setDialogOpen(true)
  }

  const handleDelete = async (id: string) => {
    try {
      setError("")
      const { error } = await supabase.from("international_transfers").delete().eq("id", id)

      if (error) {
        handleSupabaseError(error, "delete transfer")
        return
      }

      setSuccess("Transfer deleted successfully!")
      await fetchTransfers()
      setTimeout(() => setSuccess(""), 3000)
    } catch (error: any) {
      console.error("Error deleting transfer:", error)
      setError(error.message || "Failed to delete transfer")
    }
  }

  const resetForm = () => {
    setFormData({
      recipient_name: "",
      recipient_country: "India",
      amount_sent: "",
      currency_sent: "USD",
      amount_received: "",
      currency_received: "INR",
      exchange_rate: "",
      transfer_fee: "",
      transfer_method: "",
      purpose: "",
      status: "pending",
      transfer_date: new Date().toISOString().split("T")[0],
      reference_number: "",
    })
    setEditingTransfer(null)
    setError("")
  }

  // Calculate totals (primarily in USD)
  const totalSentUSD = transfers.reduce((sum, transfer) => {
    return sum + (transfer.currency_sent === "USD" ? transfer.amount_sent : 0)
  }, 0)

  const totalFeesUSD = transfers.reduce((sum, transfer) => {
    return sum + (transfer.currency_sent === "USD" ? transfer.transfer_fee : 0)
  }, 0)

  const pendingTransfers = transfers.filter((t) => t.status === "pending").length
  const totalToIndia = transfers
    .filter((t) => t.recipient_country === "India")
    .reduce((sum, t) => sum + t.amount_received, 0)

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

      {/* Summary Cards - USD focused with INR conversion */}
      <div className="grid gap-4 md:grid-cols-4">
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Total Sent</CardTitle>
            <div className="flex items-center gap-1">
              <DollarSign className="h-4 w-4 text-muted-foreground" />
              <IndianRupee className="h-3 w-3 text-muted-foreground" />
            </div>
          </CardHeader>
          <CardContent>
            <div className="space-y-1">
              <div className="text-2xl font-bold text-blue-600">{formatUSD(totalSentUSD)}</div>
              <div className="text-sm text-muted-foreground">≈ {formatINR(convertUSDToINR(totalSentUSD))}</div>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Sent to India</CardTitle>
            <span className="text-lg">🇮🇳</span>
          </CardHeader>
          <CardContent>
            <div className="space-y-1">
              <div className="text-2xl font-bold text-green-600">
                ₹{totalToIndia.toLocaleString("en-IN", { maximumFractionDigits: 2 })}
              </div>
              <div className="text-sm text-muted-foreground">Indian Rupees</div>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Transfer Fees</CardTitle>
            <TrendingUp className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="space-y-1">
              <div className="text-2xl font-bold text-orange-600">{formatUSD(totalFeesUSD)}</div>
              <div className="text-sm text-muted-foreground">≈ {formatINR(convertUSDToINR(totalFeesUSD))}</div>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Pending Transfers</CardTitle>
            <Globe className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold text-yellow-600">{pendingTransfers}</div>
            <div className="text-sm text-muted-foreground">Awaiting completion</div>
          </CardContent>
        </Card>
      </div>

      {/* Exchange Rates Card */}
      <Card>
        <CardHeader>
          <div className="flex items-center justify-between">
            <div>
              <CardTitle className="flex items-center gap-2">
                <Globe className="h-5 w-5" />
                Live Exchange Rates (USD Base)
              </CardTitle>
              <CardDescription>Current rates for international transfers</CardDescription>
            </div>
            <Button variant="outline" size="sm" onClick={fetchExchangeRates} disabled={loadingRates}>
              <RefreshCw className={`mr-2 h-4 w-4 ${loadingRates ? "animate-spin" : ""}`} />
              Refresh
            </Button>
          </div>
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-2 md:grid-cols-5 gap-4">
            {countries.map((country) => {
              const rate = exchangeRates[country.currency]
              return (
                <div key={country.code} className="text-center p-4 bg-muted/50 rounded-lg border">
                  <div className="text-2xl mb-1">{country.flag}</div>
                  <p className="text-sm font-medium">{country.name}</p>
                  <p className="text-xs text-muted-foreground">USD → {country.currency}</p>
                  <p className="text-lg font-bold text-green-600">
                    {country.currency === "INR" ? "₹" : ""}
                    {rate?.toFixed(country.currency === "INR" ? 2 : 4)}
                  </p>
                  {country.currency === "INR" && <p className="text-xs text-blue-600 font-medium">Primary Focus</p>}
                </div>
              )
            })}
          </div>
        </CardContent>
      </Card>

      {/* Transfer Management */}
      <Card>
        <CardHeader>
          <div className="flex items-center justify-between">
            <div>
              <CardTitle className="flex items-center gap-2">
                <DollarSign className="h-5 w-5" />
                International Money Transfers
              </CardTitle>
              <CardDescription>
                Track your international money transfers in USD with currency conversion
              </CardDescription>
            </div>
            <Dialog open={dialogOpen} onOpenChange={setDialogOpen}>
              <DialogTrigger asChild>
                <Button onClick={resetForm}>
                  <Plus className="mr-2 h-4 w-4" />
                  Add Transfer
                </Button>
              </DialogTrigger>
              <DialogContent className="sm:max-w-[600px] max-h-[90vh] overflow-y-auto">
                <DialogHeader>
                  <DialogTitle>{editingTransfer ? "Edit Transfer" : "Add New Transfer"}</DialogTitle>
                  <DialogDescription>
                    {editingTransfer ? "Update transfer details" : "Record a new international money transfer"}
                  </DialogDescription>
                </DialogHeader>
                <form onSubmit={handleSubmit} className="space-y-4">
                  <div className="grid grid-cols-2 gap-4">
                    <div className="space-y-2">
                      <Label htmlFor="recipient_name">Recipient Name *</Label>
                      <Input
                        id="recipient_name"
                        placeholder="Enter recipient's full name"
                        value={formData.recipient_name}
                        onChange={(e) => setFormData({ ...formData, recipient_name: e.target.value })}
                        required
                      />
                    </div>

                    <div className="space-y-2">
                      <Label htmlFor="recipient_country">Recipient Country *</Label>
                      <Select
                        value={formData.recipient_country}
                        onValueChange={(value) => {
                          const country = countries.find((c) => c.name === value)
                          setFormData({
                            ...formData,
                            recipient_country: value,
                            currency_received: country?.currency || "INR",
                          })
                        }}
                      >
                        <SelectTrigger>
                          <SelectValue placeholder="Select country" />
                        </SelectTrigger>
                        <SelectContent>
                          {countries.map((country) => (
                            <SelectItem key={country.code} value={country.name}>
                              <div className="flex items-center gap-2">
                                <span>{country.flag}</span>
                                <span>{country.name}</span>
                                <span className="text-muted-foreground">({country.currency})</span>
                              </div>
                            </SelectItem>
                          ))}
                        </SelectContent>
                      </Select>
                    </div>
                  </div>

                  <div className="grid grid-cols-2 gap-4">
                    <div className="space-y-2">
                      <Label htmlFor="amount_sent">Amount Sent (USD) *</Label>
                      <div className="relative">
                        <DollarSign className="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-muted-foreground" />
                        <Input
                          id="amount_sent"
                          type="number"
                          step="0.01"
                          placeholder="0.00"
                          className="pl-10"
                          value={formData.amount_sent}
                          onChange={(e) => {
                            const newFormData = { ...formData, amount_sent: e.target.value }
                            if (formData.exchange_rate) {
                              newFormData.amount_received = calculateReceived(e.target.value, formData.exchange_rate)
                            }
                            setFormData(newFormData)
                          }}
                          required
                        />
                      </div>
                      {formData.amount_sent && Number.parseFloat(formData.amount_sent) > 0 && (
                        <div className="text-sm text-muted-foreground">
                          ≈ {formatINR(convertUSDToINR(Number.parseFloat(formData.amount_sent)))}
                        </div>
                      )}
                    </div>

                    <div className="space-y-2">
                      <Label htmlFor="exchange_rate">Exchange Rate *</Label>
                      <div className="flex gap-2">
                        <Input
                          id="exchange_rate"
                          type="number"
                          step="0.000001"
                          placeholder="0.000000"
                          value={formData.exchange_rate}
                          onChange={(e) => {
                            const newFormData = { ...formData, exchange_rate: e.target.value }
                            if (formData.amount_sent) {
                              newFormData.amount_received = calculateReceived(formData.amount_sent, e.target.value)
                            }
                            setFormData(newFormData)
                          }}
                          required
                        />
                        <Button
                          type="button"
                          variant="outline"
                          size="sm"
                          onClick={() => {
                            const rate = exchangeRates[formData.currency_received]
                            if (rate) {
                              const newFormData = { ...formData, exchange_rate: rate.toString() }
                              if (formData.amount_sent) {
                                newFormData.amount_received = calculateReceived(formData.amount_sent, rate.toString())
                              }
                              setFormData(newFormData)
                            }
                          }}
                        >
                          Use Live Rate
                        </Button>
                      </div>
                    </div>
                  </div>

                  <div className="grid grid-cols-2 gap-4">
                    <div className="space-y-2">
                      <Label htmlFor="amount_received">Amount Received *</Label>
                      <div className="relative">
                        <span className="absolute left-3 top-1/2 transform -translate-y-1/2 text-muted-foreground text-sm">
                          {formData.currency_received === "INR" ? "₹" : formData.currency_received}
                        </span>
                        <Input
                          id="amount_received"
                          type="number"
                          step="0.01"
                          placeholder="0.00"
                          className="pl-10"
                          value={formData.amount_received}
                          onChange={(e) => setFormData({ ...formData, amount_received: e.target.value })}
                          required
                        />
                      </div>
                    </div>

                    <div className="space-y-2">
                      <Label htmlFor="transfer_fee">Transfer Fee (USD)</Label>
                      <div className="relative">
                        <DollarSign className="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-muted-foreground" />
                        <Input
                          id="transfer_fee"
                          type="number"
                          step="0.01"
                          placeholder="0.00"
                          className="pl-10"
                          value={formData.transfer_fee}
                          onChange={(e) => setFormData({ ...formData, transfer_fee: e.target.value })}
                        />
                      </div>
                    </div>
                  </div>

                  <div className="grid grid-cols-2 gap-4">
                    <div className="space-y-2">
                      <Label htmlFor="transfer_method">Transfer Method</Label>
                      <Select
                        value={formData.transfer_method}
                        onValueChange={(value) => setFormData({ ...formData, transfer_method: value })}
                      >
                        <SelectTrigger>
                          <SelectValue placeholder="Select method" />
                        </SelectTrigger>
                        <SelectContent>
                          {transferMethods.map((method) => (
                            <SelectItem key={method} value={method}>
                              {method}
                            </SelectItem>
                          ))}
                        </SelectContent>
                      </Select>
                    </div>

                    <div className="space-y-2">
                      <Label htmlFor="purpose">Purpose</Label>
                      <Select
                        value={formData.purpose}
                        onValueChange={(value) => setFormData({ ...formData, purpose: value })}
                      >
                        <SelectTrigger>
                          <SelectValue placeholder="Select purpose" />
                        </SelectTrigger>
                        <SelectContent>
                          {transferPurposes.map((purpose) => (
                            <SelectItem key={purpose} value={purpose}>
                              {purpose}
                            </SelectItem>
                          ))}
                        </SelectContent>
                      </Select>
                    </div>
                  </div>

                  <div className="grid grid-cols-2 gap-4">
                    <div className="space-y-2">
                      <Label htmlFor="transfer_date">Transfer Date *</Label>
                      <Input
                        id="transfer_date"
                        type="date"
                        value={formData.transfer_date}
                        onChange={(e) => setFormData({ ...formData, transfer_date: e.target.value })}
                        required
                      />
                    </div>

                    <div className="space-y-2">
                      <Label htmlFor="status">Status</Label>
                      <Select
                        value={formData.status}
                        onValueChange={(value) => setFormData({ ...formData, status: value })}
                      >
                        <SelectTrigger>
                          <SelectValue />
                        </SelectTrigger>
                        <SelectContent>
                          <SelectItem value="pending">Pending</SelectItem>
                          <SelectItem value="completed">Completed</SelectItem>
                          <SelectItem value="failed">Failed</SelectItem>
                        </SelectContent>
                      </Select>
                    </div>
                  </div>

                  <div className="space-y-2">
                    <Label htmlFor="reference_number">Reference Number</Label>
                    <Input
                      id="reference_number"
                      placeholder="Transfer reference number (optional)"
                      value={formData.reference_number}
                      onChange={(e) => setFormData({ ...formData, reference_number: e.target.value })}
                    />
                  </div>

                  <div className="flex justify-end space-x-2 pt-4">
                    <Button type="button" variant="outline" onClick={() => setDialogOpen(false)}>
                      Cancel
                    </Button>
                    <Button type="submit">{editingTransfer ? "Update" : "Add"} Transfer</Button>
                  </div>
                </form>
              </DialogContent>
            </Dialog>
          </div>
        </CardHeader>
        <CardContent>
          {transfers.length === 0 ? (
            <div className="text-center py-12">
              <div className="text-6xl mb-4">💸</div>
              <Send className="mx-auto h-12 w-12 text-muted-foreground mb-4" />
              <h3 className="text-lg font-semibold text-gray-900 mb-2">No transfers recorded</h3>
              <p className="text-sm text-gray-500 mb-4">Start tracking your international money transfers in USD</p>
              <Button onClick={() => setDialogOpen(true)}>
                <Plus className="mr-2 h-4 w-4" />
                Add Your First Transfer
              </Button>
            </div>
          ) : (
            <div className="overflow-x-auto">
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Recipient & Country</TableHead>
                    <TableHead>Amount Sent (USD)</TableHead>
                    <TableHead>Amount Received</TableHead>
                    <TableHead>Exchange Rate</TableHead>
                    <TableHead>Fee (USD)</TableHead>
                    <TableHead>Status</TableHead>
                    <TableHead>Date</TableHead>
                    <TableHead className="text-right">Actions</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {transfers.map((transfer) => {
                    const country = countries.find((c) => c.name === transfer.recipient_country)
                    return (
                      <TableRow key={transfer.id}>
                        <TableCell>
                          <div>
                            <p className="font-medium">{transfer.recipient_name}</p>
                            <div className="flex items-center gap-1 text-sm text-muted-foreground">
                              <span>{country?.flag}</span>
                              <span>{transfer.recipient_country}</span>
                            </div>
                          </div>
                        </TableCell>
                        <TableCell>
                          <div className="space-y-1">
                            <div className="text-red-600 font-semibold">{formatUSD(transfer.amount_sent)}</div>
                            <div className="text-xs text-muted-foreground">
                              ≈ {formatINR(convertUSDToINR(transfer.amount_sent))}
                            </div>
                          </div>
                        </TableCell>
                        <TableCell className="text-green-600 font-semibold">
                          {transfer.currency_received === "INR"
                            ? `₹${transfer.amount_received.toLocaleString("en-IN", { maximumFractionDigits: 2 })}`
                            : formatTransferCurrency(transfer.amount_received, transfer.currency_received, false)}
                        </TableCell>
                        <TableCell>
                          <span className="font-mono text-sm">
                            {transfer.exchange_rate.toFixed(transfer.currency_received === "INR" ? 2 : 4)}
                          </span>
                        </TableCell>
                        <TableCell>
                          <div className="space-y-1">
                            <div className="text-orange-600 font-semibold">{formatUSD(transfer.transfer_fee)}</div>
                            <div className="text-xs text-muted-foreground">
                              ≈ {formatINR(convertUSDToINR(transfer.transfer_fee))}
                            </div>
                          </div>
                        </TableCell>
                        <TableCell>
                          <Badge
                            variant={
                              transfer.status === "completed"
                                ? "default"
                                : transfer.status === "pending"
                                  ? "secondary"
                                  : "destructive"
                            }
                          >
                            {transfer.status.charAt(0).toUpperCase() + transfer.status.slice(1)}
                          </Badge>
                        </TableCell>
                        <TableCell>{new Date(transfer.transfer_date).toLocaleDateString()}</TableCell>
                        <TableCell className="text-right">
                          <div className="flex justify-end space-x-2">
                            <Button variant="ghost" size="sm" onClick={() => handleEdit(transfer)}>
                              <Edit className="h-4 w-4" />
                            </Button>
                            <Button variant="ghost" size="sm" onClick={() => handleDelete(transfer.id)}>
                              <Trash2 className="h-4 w-4" />
                            </Button>
                          </div>
                        </TableCell>
                      </TableRow>
                    )
                  })}
                </TableBody>
              </Table>
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  )
}
