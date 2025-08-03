import MonthlyExpenseTracker from "@/components/dashboard/monthly-expense-tracker"

export default function ExpensesPage() {
  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold tracking-tight">Monthly Expenses</h1>
        <p className="text-muted-foreground">Track your current month's expenses and budget</p>
      </div>
      <MonthlyExpenseTracker />
    </div>
  )
}
