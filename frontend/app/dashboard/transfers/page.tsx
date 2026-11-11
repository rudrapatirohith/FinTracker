import InternationalTransfers from "@/components/dashboard/international-transfers"

export default function TransfersPage() {
  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold tracking-tight">International Transfers</h1>
        <p className="text-muted-foreground">Track your international money transfers with real-time exchange rates</p>
      </div>
      <InternationalTransfers />
    </div>
  )
}
