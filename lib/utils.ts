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

// Live exchange rates - you can update these or fetch from an API
export const EXCHANGE_RATES = {
  USD_TO_INR: 83.25,
  INR_TO_USD: 0.012,
}

export function convertUSDToINR(usdAmount: number): number {
  return usdAmount * EXCHANGE_RATES.USD_TO_INR
}

export function convertINRToUSD(inrAmount: number): number {
  return inrAmount * EXCHANGE_RATES.INR_TO_USD
}

export function convertCurrency(amount: number, fromCurrency: string, toCurrency: string): number {
  if (fromCurrency === toCurrency) return amount

  if (fromCurrency === "USD" && toCurrency === "INR") {
    return convertUSDToINR(amount)
  }

  if (fromCurrency === "INR" && toCurrency === "USD") {
    return convertINRToUSD(amount)
  }

  return amount
}

// Format dual currency display (USD primary with INR equivalent)
export function formatDualCurrency(usdAmount: number): string {
  const inrAmount = convertUSDToINR(usdAmount)
  return `${formatUSD(usdAmount)} (${formatINR(inrAmount)})`
}

// Format dual currency for transfers (USD primary with optional INR)
export function formatTransferCurrency(amount: number, currency: string, showINR = true): string {
  if (currency === "USD" && showINR) {
    const inrAmount = convertUSDToINR(amount)
    return `${formatUSD(amount)} (≈ ${formatINR(inrAmount)})`
  }
  return formatCurrency(amount, currency)
}
