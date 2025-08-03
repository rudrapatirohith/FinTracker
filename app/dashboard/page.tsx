import DashboardOverview from "@/components/dashboard/dashboard-overview"

export default function DashboardPage() {
  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold tracking-tight">Dashboard</h1>
        <p className="text-muted-foreground">Overview of your financial status and recent activity</p>
      </div>
      <DashboardOverview />
    </div>
  )
}
