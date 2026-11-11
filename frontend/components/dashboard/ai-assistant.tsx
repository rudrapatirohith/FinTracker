"use client"

import type React from "react"
import { useEffect, useRef, useState, useCallback } from "react"
import { supabase } from "@/lib/supabase"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { ScrollArea } from "@/components/ui/scroll-area"
import { toUSD, formatUSDDisplay } from "@/lib/utils"
import { Bot, X, Send, Mic, MicOff, Loader2, MessageSquare } from "lucide-react"
import { cn } from "@/lib/utils"

interface Message {
  role: "user" | "assistant"
  content: string
}

// Extend window type for SpeechRecognition
declare global {
  interface Window {
    SpeechRecognition: typeof SpeechRecognition
    webkitSpeechRecognition: typeof SpeechRecognition
  }
}

export default function AIAssistant() {
  const [open, setOpen] = useState(false)
  const [messages, setMessages] = useState<Message[]>([])
  const [input, setInput] = useState("")
  const [loading, setLoading] = useState(false)
  const [listening, setListening] = useState(false)
  const [financialContext, setFinancialContext] = useState("")
  const recognitionRef = useRef<SpeechRecognition | null>(null)
  const scrollRef = useRef<HTMLDivElement>(null)
  const inputRef = useRef<HTMLInputElement>(null)

  const GEMINI_API_KEY = process.env.NEXT_PUBLIC_GEMINI_API_KEY

  // Fetch financial context once panel opens
  useEffect(() => {
    if (open && !financialContext) {
      buildFinancialContext()
    }
  }, [open])

  // Auto-scroll to bottom on new messages
  useEffect(() => {
    if (scrollRef.current) {
      scrollRef.current.scrollTop = scrollRef.current.scrollHeight
    }
  }, [messages, loading])

  const buildFinancialContext = async () => {
    try {
      const {
        data: { user },
      } = await supabase.auth.getUser()
      if (!user) return

      const now = new Date()
      const firstOfMonth = new Date(now.getFullYear(), now.getMonth(), 1).toISOString().split("T")[0]
      const sixMonthsAgo = new Date()
      sixMonthsAgo.setMonth(sixMonthsAgo.getMonth() - 6)

      const [incomeRes, transferRes, loanRes, loanPayRes, scheduledRes, expensesRes] = await Promise.all([
        supabase.from("income").select("amount, currency, inr_equivalent, category, date").eq("user_id", user.id),
        supabase.from("international_transfers").select("amount_sent, currency_sent, inr_equivalent, transfer_date").eq("user_id", user.id),
        supabase.from("loans").select("loan_name, current_balance, currency, status").eq("user_id", user.id),
        supabase.from("loan_payments").select("amount, payment_date").eq("user_id", user.id).gte("payment_date", sixMonthsAgo.toISOString().split("T")[0]),
        supabase.from("scheduled_payments").select("payment_name, amount, currency, category, due_date, status").eq("user_id", user.id),
        supabase.from("monthly_expenses").select("inr_equivalent, category, expense_date").eq("user_id", user.id).gte("expense_date", firstOfMonth),
      ])

      const totalIncomeUSD = (incomeRes.data || []).reduce((s, i) => {
        return s + (i.inr_equivalent ? toUSD(i.inr_equivalent, "INR") : toUSD(Number(i.amount), i.currency || "USD"))
      }, 0)

      const totalTransfersUSD = (transferRes.data || []).reduce((s, i) => {
        return s + (i.inr_equivalent ? toUSD(i.inr_equivalent, "INR") : toUSD(Number(i.amount_sent), i.currency_sent || "USD"))
      }, 0)

      const totalLoanPaymentsUSD = (loanPayRes.data || []).reduce((s, i) => s + toUSD(Number(i.amount), "USD"), 0)

      const pendingPaymentsUSD = (scheduledRes.data || [])
        .filter((p) => p.status === "pending")
        .reduce((s, p) => s + toUSD(Number(p.amount), p.currency || "USD"), 0)

      const monthExpensesUSD = (expensesRes.data || []).reduce((s, e) => s + toUSD(e.inr_equivalent || 0, "INR"), 0)

      const totalExpensesUSD = totalTransfersUSD + totalLoanPaymentsUSD

      // Category breakdown of scheduled payments
      const catMap: Record<string, number> = {}
      ;(scheduledRes.data || []).forEach((p) => {
        const cat = p.category || "Other"
        catMap[cat] = (catMap[cat] || 0) + toUSD(Number(p.amount), p.currency || "USD")
      })
      const topCats = Object.entries(catMap)
        .sort((a, b) => b[1] - a[1])
        .slice(0, 5)
        .map(([cat, amt]) => `${cat}: ${formatUSDDisplay(amt)}`)
        .join(", ")

      const activeLoans = (loanRes.data || []).filter((l) => l.status === "active")
      const loanSummary = activeLoans
        .map((l) => `${l.loan_name} (${formatUSDDisplay(toUSD(Number(l.current_balance), l.currency || "USD"))} remaining)`)
        .join("; ")

      const ctx = `
User Financial Summary (all values in USD):
- Total all-time income: ${formatUSDDisplay(totalIncomeUSD)}
- Total all-time international transfers sent: ${formatUSDDisplay(totalTransfersUSD)}
- Total loan payments (last 6 months): ${formatUSDDisplay(totalLoanPaymentsUSD)}
- Total expenses (all time): ${formatUSDDisplay(totalExpensesUSD)}
- Current month expenses: ${formatUSDDisplay(monthExpensesUSD)}
- Pending scheduled payments: ${formatUSDDisplay(pendingPaymentsUSD)}
- Net savings estimate: ${formatUSDDisplay(totalIncomeUSD - totalExpensesUSD)}
- Active loans: ${loanSummary || "None"}
- Top spending categories: ${topCats || "No data"}
`.trim()

      setFinancialContext(ctx)
    } catch (err) {
      console.error("Error building financial context:", err)
    }
  }

  const sendMessage = useCallback(
    async (text: string) => {
      if (!text.trim()) return

      const userMsg: Message = { role: "user", content: text }
      const updatedMessages = [...messages, userMsg].slice(-10) // keep last 10
      setMessages(updatedMessages)
      setInput("")
      setLoading(true)

      if (!GEMINI_API_KEY) {
        setMessages((prev) => [
          ...prev,
          {
            role: "assistant",
            content: "Add GEMINI_API_KEY to your .env.local to enable AI",
          },
        ])
        setLoading(false)
        return
      }

      try {
        const systemPrompt = `You are a friendly and knowledgeable personal financial advisor. 
Answer questions about the user's finances based on the data below. 
Be specific, actionable, and concise. Always display amounts in USD.

${financialContext}`

        // Build Gemini contents array
        const contents = [
          {
            role: "user",
            parts: [{ text: systemPrompt + "\n\nUser: " + text }],
          },
          // For multi-turn, include prior messages
          ...updatedMessages.slice(0, -1).flatMap((m) => [
            {
              role: m.role === "user" ? "user" : "model",
              parts: [{ text: m.content }],
            },
          ]),
          {
            role: "user",
            parts: [{ text: text }],
          },
        ]

        const response = await fetch(
          `https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=${GEMINI_API_KEY}`,
          {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({
              contents: [
                {
                  role: "user",
                  parts: [
                    {
                      text: `${systemPrompt}\n\n${updatedMessages
                        .map((m) => `${m.role === "user" ? "User" : "Assistant"}: ${m.content}`)
                        .join("\n")}`,
                    },
                  ],
                },
              ],
              generationConfig: {
                temperature: 0.7,
                maxOutputTokens: 512,
              },
            }),
          },
        )

        if (!response.ok) {
          throw new Error(`Gemini API error: ${response.status}`)
        }

        const result = await response.json()
        const assistantText =
          result?.candidates?.[0]?.content?.parts?.[0]?.text ||
          "I couldn't generate a response. Please try again."

        setMessages((prev) => [...prev, { role: "assistant", content: assistantText }])
      } catch (err) {
        console.error("AI assistant error:", err)
        setMessages((prev) => [
          ...prev,
          {
            role: "assistant",
            content: "Sorry, I encountered an error. Please check your API key and try again.",
          },
        ])
      } finally {
        setLoading(false)
      }
    },
    [messages, financialContext, GEMINI_API_KEY],
  )

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    sendMessage(input)
  }

  const toggleVoice = () => {
    if (listening) {
      recognitionRef.current?.stop()
      setListening(false)
      return
    }

    const SpeechRecognitionAPI = window.SpeechRecognition || window.webkitSpeechRecognition
    if (!SpeechRecognitionAPI) {
      alert("Voice input is not supported in your browser.")
      return
    }

    const recognition = new SpeechRecognitionAPI()
    recognition.lang = "en-US"
    recognition.interimResults = false
    recognition.maxAlternatives = 1

    recognition.onresult = (event) => {
      const transcript = event.results[0][0].transcript
      setInput(transcript)
      setListening(false)
    }

    recognition.onerror = () => {
      setListening(false)
    }

    recognition.onend = () => {
      setListening(false)
    }

    recognition.start()
    recognitionRef.current = recognition
    setListening(true)
  }

  const suggestedQuestions = [
    "How much did I spend this month?",
    "Where can I cut costs?",
    "Give me a savings plan",
  ]

  return (
    <>
      {/* Floating Button */}
      <button
        onClick={() => setOpen((prev) => !prev)}
        className={cn(
          "fixed bottom-6 right-6 z-50 w-14 h-14 rounded-full shadow-lg flex items-center justify-center transition-all duration-200",
          "bg-blue-600 hover:bg-blue-700 text-white",
          open && "rotate-12 scale-90",
        )}
        aria-label="Open AI Financial Assistant"
      >
        {open ? <X className="h-6 w-6" /> : <Bot className="h-6 w-6" />}
      </button>

      {/* Slide-up Panel */}
      <div
        className={cn(
          "fixed bottom-24 right-6 z-50 w-[380px] max-w-[calc(100vw-3rem)] rounded-2xl shadow-2xl flex flex-col overflow-hidden",
          "bg-background border border-border",
          "transition-all duration-300 ease-in-out",
          open ? "opacity-100 translate-y-0 pointer-events-auto" : "opacity-0 translate-y-8 pointer-events-none",
        )}
        style={{ height: "520px" }}
      >
        {/* Panel Header */}
        <div className="flex items-center gap-3 px-4 py-3 bg-blue-600 text-white flex-shrink-0">
          <div className="w-8 h-8 rounded-full bg-white/20 flex items-center justify-center">
            <Bot className="h-5 w-5" />
          </div>
          <div>
            <p className="font-semibold text-sm">AI Financial Advisor</p>
            <p className="text-xs text-blue-100">Powered by Gemini</p>
          </div>
          <button
            onClick={() => setOpen(false)}
            className="ml-auto p-1 rounded-full hover:bg-white/20 transition-colors"
            aria-label="Close"
          >
            <X className="h-4 w-4" />
          </button>
        </div>

        {/* Messages */}
        <ScrollArea className="flex-1 px-4 py-3">
          <div ref={scrollRef} className="space-y-3">
            {messages.length === 0 && (
              <div className="text-center py-4">
                <MessageSquare className="h-10 w-10 mx-auto text-muted-foreground mb-2" />
                <p className="text-sm text-muted-foreground mb-4">Ask me anything about your finances!</p>
                <div className="flex flex-col gap-2">
                  {suggestedQuestions.map((q) => (
                    <button
                      key={q}
                      onClick={() => sendMessage(q)}
                      className="text-xs text-left px-3 py-2 rounded-lg border border-border hover:bg-muted transition-colors text-foreground"
                    >
                      {q}
                    </button>
                  ))}
                </div>
              </div>
            )}

            {messages.map((msg, i) => (
              <div
                key={i}
                className={cn(
                  "flex",
                  msg.role === "user" ? "justify-end" : "justify-start",
                )}
              >
                <div
                  className={cn(
                    "max-w-[80%] rounded-2xl px-3 py-2 text-sm leading-relaxed",
                    msg.role === "user"
                      ? "bg-blue-600 text-white rounded-br-sm"
                      : "bg-muted text-foreground rounded-bl-sm",
                  )}
                >
                  {msg.content}
                </div>
              </div>
            ))}

            {loading && (
              <div className="flex justify-start">
                <div className="bg-muted rounded-2xl rounded-bl-sm px-3 py-2">
                  <div className="flex items-center gap-1">
                    <span className="w-2 h-2 rounded-full bg-muted-foreground animate-bounce" style={{ animationDelay: "0ms" }} />
                    <span className="w-2 h-2 rounded-full bg-muted-foreground animate-bounce" style={{ animationDelay: "150ms" }} />
                    <span className="w-2 h-2 rounded-full bg-muted-foreground animate-bounce" style={{ animationDelay: "300ms" }} />
                  </div>
                </div>
              </div>
            )}
          </div>
        </ScrollArea>

        {/* Input */}
        <form onSubmit={handleSubmit} className="flex items-center gap-2 px-3 py-3 border-t border-border flex-shrink-0">
          <Button
            type="button"
            variant="ghost"
            size="icon"
            onClick={toggleVoice}
            className={cn(
              "flex-shrink-0 h-9 w-9 rounded-full",
              listening && "text-red-500 animate-pulse",
            )}
            aria-label={listening ? "Stop voice input" : "Start voice input"}
          >
            {listening ? <MicOff className="h-4 w-4" /> : <Mic className="h-4 w-4" />}
          </Button>
          <Input
            ref={inputRef}
            value={input}
            onChange={(e) => setInput(e.target.value)}
            placeholder="Ask about your finances..."
            className="flex-1 h-9 text-sm rounded-full"
            disabled={loading}
          />
          <Button
            type="submit"
            size="icon"
            disabled={loading || !input.trim()}
            className="flex-shrink-0 h-9 w-9 rounded-full bg-blue-600 hover:bg-blue-700"
            aria-label="Send"
          >
            {loading ? <Loader2 className="h-4 w-4 animate-spin" /> : <Send className="h-4 w-4" />}
          </Button>
        </form>
      </div>
    </>
  )
}
