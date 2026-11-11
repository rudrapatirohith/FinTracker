import IncomeManagement from "@/components/dashboard/income-management"

export default function IncomePage() {
  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold tracking-tight">Income Management</h1>
        <p className="text-muted-foreground">Track and manage your income sources</p>
      </div>
      <IncomeManagement />
    </div>
  )
}
