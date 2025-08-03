import { type ClassValue, clsx } from "clsx"
import { twMerge } from "tailwind-merge"
import { format } from "date-fns"

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs))
}

export function formatCurrency(amount: number, currency = "USD"): string {
  if (currency === "INR") {
    return new Intl.NumberFormat("en-IN", {
      style: "currency",
      currency: "INR",
      minimumFractionDigits: 2,
      maximumFractionDigits: 2,
    }).format(amount)
  }

  return new Intl.NumberFormat("en-US", {
    style: "currency",
    currency: currency,
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(amount)
}

export function formatINR(amount: number): string {
  return new Intl.NumberFormat("en-IN", {
    style: "currency",
    currency: "INR",
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(amount)
}

export function formatUSD(amount: number): string {
  return new Intl.NumberFormat("en-US", {
    style: "currency",
    currency: "USD",
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(amount)
}

export function formatDate(date: string | Date): string {
  return format(new Date(date), "MMM d, yyyy")
}

export function formatDateTime(date: string | Date): string {
  return new Intl.DateTimeFormat("en-IN", {
    year: "numeric",
    month: "short",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  }).format(new Date(date))
}

export function calculateLoanProgress(principal: number, current: number): number {
  if (principal === 0) return 0
  return ((principal - current) / principal) * 100
}

export function generateReferenceNumber(): string {
  return "TXN" + Date.now().toString(36).toUpperCase() + Math.random().toString(36).substr(2, 5).toUpperCase()
}

// Helper function to calculate INR equivalent with manual exchange rate
export function calculateINREquivalent(usdAmount: number, exchangeRate: number): number {
  return usdAmount * exchangeRate
}

// Helper function to format dual currency display with custom rate
export function formatDualCurrency(usdAmount: number, exchangeRate: number): string {
  const inrAmount = calculateINREquivalent(usdAmount, exchangeRate)
  return `${formatUSD(usdAmount)} (₹${inrAmount.toLocaleString("en-IN", { maximumFractionDigits: 2 })} @ ₹${exchangeRate})`
}

// Helper function to get current exchange rate from API
export async function getCurrentExchangeRate(): Promise<number> {
  try {
    const response = await fetch("https://api.exchangerate-api.com/v4/latest/USD")
    const data = await response.json()
    return data.rates.INR || 83.25 // fallback rate
  } catch (error) {
    console.error("Failed to fetch exchange rate:", error)
    return 83.25 // fallback rate
  }
}

// Helper function to validate exchange rate
export function isValidExchangeRate(rate: number): boolean {
  return rate > 0 && rate < 200 // reasonable bounds for USD to INR
}
