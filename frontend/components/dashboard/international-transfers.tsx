"use client"

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
  Info,
} from "lucide-react"
import { formatUSD, formatINR, calculateINREquivalent, getCurrentExchangeRate, isValidExchangeRate } from "@/lib/utils"

interface Transfer {
  id: string
  recipientName: string
  recipientCountry: string
  amountSent: number
  currencySent: string
  amountReceived: number
  currencyReceived: string
  exchangeRate: number
  transferFee: number
  transferMethod: string | null
  purpose: string | null
  status: string
  transferDate: string
  completionDate: string | null
  referenceNumber: string | null
  usdToInrRate: number | null
  inrEquivalent: number | null
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
  const { accessToken } = useAuth()
  const [transfers, setTransfers] = useState<Transfer[]>([])
  const [loading, setLoading] = useState(true)
  const [dialogOpen, setDialogOpen] = useState(false)
  const [editingTransfer, setEditingTransfer] = useState<Transfer | null>(null)
  const [currentExchangeRate, setCurrentExchangeRate] = useState<number>(83.25)
  const [fetchingRate, setFetchingRate] = useState(false)
  const [error, setError] = useState("")
  const [success, setSuccess] = useState("")

  const [formData, setFormData] = useState({
    recipientName: "",
    recipientCountry: "India",
    amountSent: "",
    currencySent: "USD",
    amountReceived: "",
    currencyReceived: "INR",
    exchangeRate: "",
    transferFee: "",
    transferMethod: "",
    purpose: "",
    status: "pending",
    transferDate: new Date().toISOString().split("T")[0],
    referenceNumber: "",
    usdToInrRate: "",
  })

  useEffect(() => {
    fetchTransfers()
    fetchCurrentExchangeRate()
  }, [])

  const fetchCurrentExchangeRate = async () => {
    setFetchingRate(true)
    try {
      const rate = await getCurrentExchangeRate()
      setCurrentExchangeRate(rate)
      setFormData((prev) => ({
        ...prev,
        exchangeRate: rate.toString(),
        usdToInrRate: rate.toString(),
      }))
    } catch (error) {
      console.error("Failed to fetch exchange rate:", error)
    } finally {
      setFetchingRate(false)
    }
  }

  const fetchTransfers = async () => {
    if (!accessToken) {
      setError("Please sign in to view transfers.")
      setLoading(false)
      return
    }
    try {
      setError("")
      const page = await apiGetPage<Transfer>(`${API.transfers}?page=0&size=1000&sort=transferDate,desc`, accessToken)
      setTransfers(page.content)
    } catch (error) {
      console.error("Error fetching transfers:", error)
      setError(error instanceof Error ? error.message : "Failed to fetch transfers")
    } finally {
      setLoading(false)
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
    if (!accessToken) {
      setError("Your session has expired. Please sign in again.")
      return
    }
    try {
      const exchangeRate = Number.parseFloat(formData.exchangeRate)
      if (!isValidExchangeRate(exchangeRate)) {
        setError("Please enter a valid exchange rate")
        return
      }
      let usdToInrRate: number | null = null
      let inrEquivalent: number | null = null
      if (formData.currencySent === "USD") {
        const rate = Number.parseFloat(formData.usdToInrRate)
        if (!isValidExchangeRate(rate)) {
          setError("Please enter a valid USD to INR rate")
          return
        }
        usdToInrRate = rate
        inrEquivalent = calculateINREquivalent(Number.parseFloat(formData.amountSent), rate)
      }
      const transferData = {
        recipientName: formData.recipientName,
        recipientCountry: formData.recipientCountry,
        amountSent: Number.parseFloat(formData.amountSent),
        currencySent: formData.currencySent,
        amountReceived: Number.parseFloat(formData.amountReceived),
        currencyReceived: formData.currencyReceived,
        exchangeRate,
        transferFee: Number.parseFloat(formData.transferFee) || 0,
        transferMethod: formData.transferMethod || null,
        purpose: formData.purpose || null,
        status: formData.status,
        transferDate: formData.transferDate,
        completionDate: null,
        referenceNumber: formData.referenceNumber || null,
        usdToInrRate,
        inrEquivalent,
      }
      if (editingTransfer) {
        await apiRequest<Transfer>(`${API.transfers}/${editingTransfer.id}`, accessToken, {
          method: "PUT", body: JSON.stringify(transferData),
        })
      } else {
        await apiRequest<Transfer>(API.transfers, accessToken, {
          method: "POST", body: JSON.stringify(transferData),
        })
      }
      setSuccess(editingTransfer ? "Transfer updated successfully!" : "Transfer added successfully!")
      await fetchTransfers()
      setDialogOpen(false)
      resetForm()
      setTimeout(() => setSuccess(""), 3000)
    } catch (error) {
      console.error("Error saving transfer:", error)
      setError(error instanceof Error ? error.message : "Failed to save transfer")
    }
  }

  const handleEdit = (transfer: Transfer) => {
    setEditingTransfer(transfer)
    setFormData({
      recipientName: transfer.recipientName,
      recipientCountry: transfer.recipientCountry,
      amountSent: transfer.amountSent.toString(),
      currencySent: transfer.currencySent,
      amountReceived: transfer.amountReceived.toString(),
      currencyReceived: transfer.currencyReceived,
      exchangeRate: transfer.exchangeRate.toString(),
      transferFee: transfer.transferFee.toString(),
      transferMethod: transfer.transferMethod || "",
      purpose: transfer.purpose || "",
      status: transfer.status,
      transferDate: transfer.transferDate,
      referenceNumber: transfer.referenceNumber || "",
      usdToInrRate: transfer.usdToInrRate?.toString() || "",
    })
    setDialogOpen(true)
  }

  const handleDelete = async (id: string) => {
    if (!accessToken) {
      setError("Your session has expired. Please sign in again.")
      return
    }
    try {
      setError("")
      await apiRequest<void>(`${API.transfers}/${id}`, accessToken, { method: "DELETE" })
      setSuccess("Transfer deleted successfully!")
      await fetchTransfers()
      setTimeout(() => setSuccess(""), 3000)
    } catch (error) {
      console.error("Error deleting transfer:", error)
      setError(error instanceof Error ? error.message : "Failed to delete transfer")
    }
  }

  const resetForm = () => {
    setFormData({
      recipientName: "",
      recipientCountry: "India",
      amountSent: "",
      currencySent: "USD",
      amountReceived: "",
      currencyReceived: "INR",
      exchangeRate: currentExchangeRate.toString(),
      transferFee: "",
      transferMethod: "",
      purpose: "",
      status: "pending",
      transferDate: new Date().toISOString().split("T")[0],
      referenceNumber: "",
      usdToInrRate: currentExchangeRate.toString(),
    })
    setEditingTransfer(null)
    setError("")
  }

  // Calculate totals using INR equivalents
  const totalSentINR = transfers.reduce((sum, transfer) => {
    return sum + (transfer.inrEquivalent || 0)
  }, 0)

  const totalFeesINR = transfers.reduce((sum, transfer) => {
    if (transfer.currencySent === "USD" && transfer.usdToInrRate) {
      return sum + calculateINREquivalent(transfer.transferFee, transfer.usdToInrRate)
    }
    return sum + transfer.transferFee
  }, 0)

  const pendingTransfers = transfers.filter((t) => t.status === "pending").length
  const totalToIndia = transfers
    .filter((t) => t.recipientCountry === "India")
    .reduce((sum, t) => sum + t.amountReceived, 0)

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

      {/* Exchange Rate Info */}
      <Alert>
        <Info className="h-4 w-4" />
        <AlertDescription>
          <strong>Exchange Rate Policy:</strong> Enter the actual exchange rate used for each transfer to maintain
          historical accuracy. Current live USD to INR rate: ₹{currentExchangeRate} per $1.
        </AlertDescription>
      </Alert>

      {/* Summary Cards */}
      <div className="grid gap-4 md:grid-cols-4">
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Total Sent (INR)</CardTitle>
            <IndianRupee className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold text-blue-600">{formatINR(totalSentINR)}</div>
            <div className="text-sm text-muted-foreground">All transfers in INR equivalent</div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Sent to India</CardTitle>
            <span className="text-lg">🇮🇳</span>
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold text-green-600">
              ₹{totalToIndia.toLocaleString("en-IN", { maximumFractionDigits: 2 })}
            </div>
            <div className="text-sm text-muted-foreground">Indian Rupees received</div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Transfer Fees (INR)</CardTitle>
            <TrendingUp className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold text-orange-600">{formatINR(totalFeesINR)}</div>
            <div className="text-sm text-muted-foreground">Total fees paid</div>
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

      {/* Transfer Management */}
      <Card>
        <CardHeader>
          <div className="flex items-center justify-between">
            <div>
              <CardTitle className="flex items-center gap-2">
                <Send className="h-5 w-5" />
                International Money Transfers
              </CardTitle>
              <CardDescription>
                Track international transfers with accurate exchange rates for historical transparency
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
                    {editingTransfer
                      ? "Update transfer details"
                      : "Record a new international money transfer with accurate exchange rates"}
                  </DialogDescription>
                </DialogHeader>
                <form onSubmit={handleSubmit} className="space-y-4">
                  <div className="grid grid-cols-2 gap-4">
                    <div className="space-y-2">
                      <Label htmlFor="recipientName">Recipient Name *</Label>
                      <Input
                        id="recipientName"
                        placeholder="Enter recipient's full name"
                        value={formData.recipientName}
                        onChange={(e) => setFormData({ ...formData, recipientName: e.target.value })}
                        required
                      />
                    </div>

                    <div className="space-y-2">
                      <Label htmlFor="recipientCountry">Recipient Country *</Label>
                      <Select
                        value={formData.recipientCountry}
                        onValueChange={(value) => {
                          const country = countries.find((c) => c.name === value)
                          setFormData({
                            ...formData,
                            recipientCountry: value,
                            currencyReceived: country?.currency || "INR",
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
                      <Label htmlFor="amountSent">Amount Sent (USD) *</Label>
                      <div className="relative">
                        <DollarSign className="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-muted-foreground" />
                        <Input
                          id="amountSent"
                          type="number"
                          step="0.01"
                          placeholder="0.00"
                          className="pl-10"
                          value={formData.amountSent}
                          onChange={(e) => {
                            const newFormData = { ...formData, amountSent: e.target.value }
                            if (formData.exchangeRate) {
                              newFormData.amountReceived = calculateReceived(e.target.value, formData.exchangeRate)
                            }
                            setFormData(newFormData)
                          }}
                          required
                        />
                      </div>
                    </div>

                    <div className="space-y-2">
                      <Label htmlFor="exchangeRate">Exchange Rate (USD to {formData.currencyReceived}) *</Label>
                      <div className="flex gap-2">
                        <Input
                          id="exchangeRate"
                          type="number"
                          step="0.000001"
                          placeholder="0.000000"
                          value={formData.exchangeRate}
                          onChange={(e) => {
                            const newFormData = { ...formData, exchangeRate: e.target.value }
                            if (formData.amountSent) {
                              newFormData.amountReceived = calculateReceived(formData.amountSent, e.target.value)
                            }
                            setFormData(newFormData)
                          }}
                          required
                        />
                        <Button
                          type="button"
                          variant="outline"
                          size="sm"
                          onClick={fetchCurrentExchangeRate}
                          disabled={fetchingRate}
                        >
                          <RefreshCw className={`mr-2 h-4 w-4 ${fetchingRate ? "animate-spin" : ""}`} />
                          Live
                        </Button>
                      </div>
                    </div>
                  </div>

                  {formData.currencySent === "USD" && (
                    <div className="space-y-2">
                      <Label htmlFor="usdToInrRate">USD to INR Rate (for tracking) *</Label>
                      <Input
                        id="usdToInrRate"
                        type="number"
                        step="0.000001"
                        placeholder="83.25"
                        value={formData.usdToInrRate}
                        onChange={(e) => setFormData({ ...formData, usdToInrRate: e.target.value })}
                        required
                      />
                      {formData.amountSent &&
                        formData.usdToInrRate &&
                        Number.parseFloat(formData.amountSent) > 0 &&
                        Number.parseFloat(formData.usdToInrRate) > 0 && (
                          <div className="text-sm text-muted-foreground bg-muted p-2 rounded">
                            <div className="flex items-center gap-1">
                              <IndianRupee className="h-3 w-3" />
                              <span>
                                INR Equivalent:{" "}
                                {formatINR(
                                  calculateINREquivalent(
                                    Number.parseFloat(formData.amountSent),
                                    Number.parseFloat(formData.usdToInrRate),
                                  ),
                                )}
                              </span>
                            </div>
                          </div>
                        )}
                    </div>
                  )}

                  <div className="grid grid-cols-2 gap-4">
                    <div className="space-y-2">
                      <Label htmlFor="amountReceived">Amount Received *</Label>
                      <div className="relative">
                        <span className="absolute left-3 top-1/2 transform -translate-y-1/2 text-muted-foreground text-sm">
                          {formData.currencyReceived === "INR" ? "₹" : formData.currencyReceived}
                        </span>
                        <Input
                          id="amountReceived"
                          type="number"
                          step="0.01"
                          placeholder="0.00"
                          className="pl-10"
                          value={formData.amountReceived}
                          onChange={(e) => setFormData({ ...formData, amountReceived: e.target.value })}
                          required
                        />
                      </div>
                    </div>

                    <div className="space-y-2">
                      <Label htmlFor="transferFee">Transfer Fee (USD)</Label>
                      <div className="relative">
                        <DollarSign className="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-muted-foreground" />
                        <Input
                          id="transferFee"
                          type="number"
                          step="0.01"
                          placeholder="0.00"
                          className="pl-10"
                          value={formData.transferFee}
                          onChange={(e) => setFormData({ ...formData, transferFee: e.target.value })}
                        />
                      </div>
                    </div>
                  </div>

                  <div className="grid grid-cols-2 gap-4">
                    <div className="space-y-2">
                      <Label htmlFor="transferMethod">Transfer Method</Label>
                      <Select
                        value={formData.transferMethod}
                        onValueChange={(value) => setFormData({ ...formData, transferMethod: value })}
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
                      <Label htmlFor="transferDate">Transfer Date *</Label>
                      <Input
                        id="transferDate"
                        type="date"
                        value={formData.transferDate}
                        onChange={(e) => setFormData({ ...formData, transferDate: e.target.value })}
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
                    <Label htmlFor="referenceNumber">Reference Number</Label>
                    <Input
                      id="referenceNumber"
                      placeholder="Transfer reference number (optional)"
                      value={formData.referenceNumber}
                      onChange={(e) => setFormData({ ...formData, referenceNumber: e.target.value })}
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
              <Send className="mx-auto h-12 w-12 text-muted-foreground mb-4" />
              <h3 className="text-lg font-semibold text-gray-900 mb-2">No transfers recorded</h3>
              <p className="text-sm text-gray-500 mb-4">Start tracking your international money transfers</p>
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
                    <TableHead>Amount Sent</TableHead>
                    <TableHead>Amount Received</TableHead>
                    <TableHead>Exchange Rate</TableHead>
                    <TableHead>INR Equivalent</TableHead>
                    <TableHead>Status</TableHead>
                    <TableHead>Date</TableHead>
                    <TableHead className="text-right">Actions</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {transfers.map((transfer) => {
                    const country = countries.find((c) => c.name === transfer.recipientCountry)
                    return (
                      <TableRow key={transfer.id}>
                        <TableCell>
                          <div>
                            <p className="font-medium">{transfer.recipientName}</p>
                            <div className="flex items-center gap-1 text-sm text-muted-foreground">
                              <span>{country?.flag}</span>
                              <span>{transfer.recipientCountry}</span>
                            </div>
                          </div>
                        </TableCell>
                        <TableCell className="text-red-600 font-semibold">{formatUSD(transfer.amountSent)}</TableCell>
                        <TableCell className="text-green-600 font-semibold">
                          {transfer.currencyReceived === "INR"
                            ? `₹${transfer.amountReceived.toLocaleString("en-IN", { maximumFractionDigits: 2 })}`
                            : `${transfer.currencyReceived} ${transfer.amountReceived.toLocaleString()}`}
                        </TableCell>
                        <TableCell>
                          <Badge variant="outline">
                            {transfer.exchangeRate.toFixed(transfer.currencyReceived === "INR" ? 2 : 4)}
                          </Badge>
                        </TableCell>
                        <TableCell className="text-blue-600 font-semibold">
                          {transfer.inrEquivalent ? formatINR(transfer.inrEquivalent) : "—"}
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
                        <TableCell>{new Date(transfer.transferDate).toLocaleDateString()}</TableCell>
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
