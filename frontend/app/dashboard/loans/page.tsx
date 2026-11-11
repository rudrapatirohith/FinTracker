import LoanTracker from "@/components/dashboard/loan-tracker"

export default function LoansPage() {
  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold tracking-tight">Loan Management</h1>
        <p className="text-muted-foreground">Track your loans and repayment progress</p>
      </div>
      <LoanTracker />
    </div>
  )
}
