"use client";

/**
 * Dashboard Overview
 *
 * Displays the user's financial summary and quick navigation actions.
 *
 * Authentication:
 *   - Managed by the global Spring Boot AuthProvider.
 *   - The authenticated user ID comes from useAuth().
 *
 * Financial data is loaded through owner-scoped Spring Boot APIs. The browser
 * never supplies a user ID; Spring derives ownership from the validated token.
 */

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";

import { useAuth } from "@/components/auth/auth-provider";
import { API, apiGetPage } from "@/lib/api";

import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";

import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Alert, AlertDescription } from "@/components/ui/alert";

import {
  DollarSign,
  CreditCard,
  Plus,
  FileText,
  BarChart3,
  AlertCircle,
  Send,
  Calendar,
  History,
  ShoppingCart,
  ArrowUpDown,
} from "lucide-react";

import { toUSD, formatUSDDisplay } from "@/lib/utils";

// -----------------------------------------------------------------------------
// 1. Dashboard data structure
// -----------------------------------------------------------------------------

interface DashboardData {
  totalIncome: number;
  totalLoans: number;
  totalTransfers: number;
  totalExpenses: number;
  activeLoans: number;
  recentTransactions: any[];
  monthlyIncome: number;
  monthlyExpenses: number;
}

// -----------------------------------------------------------------------------
// 2. Dashboard component
// -----------------------------------------------------------------------------

type DashboardIncomeRow = { inrEquivalent: number | null }
type DashboardLoanRow = { currentBalance: number; currency: string; status: string }
type DashboardTransferRow = { inrEquivalent: number | null }
type DashboardExpenseRow = { inrEquivalent: number | null; expenseDate: string }

export default function DashboardOverview() {
  const router = useRouter();

  /**
   * Read the authenticated user from our Spring Boot AuthProvider.
   *
   * We no longer ask Supabase to identify the logged-in user.
   */
  const { user, accessToken, isLoading: authLoading } = useAuth();

  // Dashboard financial data.
  const [data, setData] = useState<DashboardData>({
    totalIncome: 0,
    totalLoans: 0,
    totalTransfers: 0,
    totalExpenses: 0,
    activeLoans: 0,
    recentTransactions: [],
    monthlyIncome: 0,
    monthlyExpenses: 0,
  });

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  // ---------------------------------------------------------------------------
  // 3. Load dashboard data after authentication is restored
  // ---------------------------------------------------------------------------

  useEffect(() => {
    /**
     * Do not fetch financial data while authentication is being restored.
     *
     * Otherwise, the component might try to fetch data before the
     * AuthProvider has identified the logged-in user.
     */
    if (authLoading) {
      return;
    }

    /**
     * No authenticated user means there is no user ID to query.
     */
    if (!user) {
      setError("Please log in to view dashboard.");
      setLoading(false);
      return;
    }

    // Load financial data for the authenticated backend user.
    if (!accessToken) {
      setError("Your session has expired. Please sign in again.");
      setLoading(false);
      return;
    }
    void fetchDashboardData(accessToken);
  }, [authLoading, user, accessToken]);

  // ---------------------------------------------------------------------------
  // 4. Fetch financial data
  // ---------------------------------------------------------------------------

  /**
   * Fetches the first bounded page from each owner-scoped resource. The backend
   * enforces ownership from the bearer token; no user ID is sent by this client.
   */
  const fetchDashboardData = async (token: string) => {
    try {
      setLoading(true);
      setError("");

      const [incomePage, loanPage, transferPage, expensePage] = await Promise.all([
        apiGetPage<DashboardIncomeRow>(`${API.income}?page=0&size=1000&sort=date,desc`, token),
        apiGetPage<DashboardLoanRow>(`${API.loans}?page=0&size=1000&sort=createdAt,desc`, token),
        apiGetPage<DashboardTransferRow>(`${API.transfers}?page=0&size=1000&sort=transferDate,desc`, token),
        apiGetPage<DashboardExpenseRow>(`${API.monthlyExpenses}?page=0&size=1000&sort=expenseDate,desc`, token),
      ]);

      const incomeData = incomePage.content;
      const loansData = loanPage.content;
      const transfersData = transferPage.content;
      const currentDate = new Date();
      const firstDay = new Date(currentDate.getFullYear(), currentDate.getMonth(), 1)
        .toISOString().slice(0, 10);
      const lastDay = new Date(currentDate.getFullYear(), currentDate.getMonth() + 1, 0)
        .toISOString().slice(0, 10);
      const expensesData = expensePage.content.filter(
        (expense) => expense.expenseDate >= firstDay && expense.expenseDate <= lastDay
      );

      const totalIncome = incomeData.reduce((sum, item) => sum + Number(item.inrEquivalent ?? 0), 0);
      const totalLoans = loansData.reduce((sum, item) => {
        const amountInINR = item.currency === "USD" ? item.currentBalance * 83.25 : item.currentBalance;
        return sum + amountInINR;
      }, 0);
      const totalTransfers = transfersData.reduce((sum, item) => sum + Number(item.inrEquivalent ?? 0), 0);
      const totalExpenses = expensesData.reduce((sum, item) => sum + Number(item.inrEquivalent ?? 0), 0);
      const activeLoans = loansData.filter((loan) => loan.status === "active").length;

      setData({
        totalIncome,
        totalLoans,
        totalTransfers,
        totalExpenses,
        activeLoans,
        recentTransactions: [],
        monthlyIncome: totalIncome,
        monthlyExpenses: totalExpenses,
      });
    } catch (error: unknown) {
      console.error("Error fetching dashboard data:", error);
      setError(error instanceof Error ? error.message : "Failed to fetch dashboard data.");
    } finally {
      setLoading(false);
    }
  };

  // ---------------------------------------------------------------------------
  // 6. Dashboard navigation
  // ---------------------------------------------------------------------------

  const handleNavigation = (path: string) => {
    router.push(path);
  };

  // ---------------------------------------------------------------------------
  // 7. Loading state
  // ---------------------------------------------------------------------------

  if (authLoading || loading) {
    return (
      <div className="space-y-6">
        <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
          {[...Array(4)].map((_, i) => (
            <Card key={i}>
              <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                <div className="h-4 bg-muted rounded w-24 animate-pulse" />
                <div className="h-4 w-4 bg-muted rounded animate-pulse" />
              </CardHeader>

              <CardContent>
                <div className="h-8 bg-muted rounded w-32 animate-pulse mb-2" />
                <div className="h-3 bg-muted rounded w-20 animate-pulse" />
              </CardContent>
            </Card>
          ))}
        </div>
      </div>
    );
  }

  // ---------------------------------------------------------------------------
  // 8. Dashboard UI
  // -----------------------------------------------------------------------------

  return (
    <div className="space-y-6">

      {/* Error Message */}
      {error && (
        <Alert variant="destructive">
          <AlertCircle className="h-4 w-4" />
          <AlertDescription>{error}</AlertDescription>
        </Alert>
      )}

      {/* Overview Cards */}
      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
        <Card
          className="cursor-pointer hover:shadow-lg transition-all duration-200 hover:scale-105 border-l-4 border-l-green-500"
          onClick={() => handleNavigation("/dashboard/income")}
        >
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Total Income</CardTitle>
            <DollarSign className="h-4 w-4 text-green-600" />
          </CardHeader>

          <CardContent>
            <div className="text-2xl font-bold text-green-600">
              {formatUSDDisplay(toUSD(data.totalIncome, "INR"))}
            </div>
            <div className="text-sm text-muted-foreground">Total Income</div>
          </CardContent>
        </Card>

        <Card
          className="cursor-pointer hover:shadow-lg transition-all duration-200 hover:scale-105 border-l-4 border-l-red-500"
          onClick={() => handleNavigation("/dashboard/loans")}
        >
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Total Debt</CardTitle>
            <CreditCard className="h-4 w-4 text-red-600" />
          </CardHeader>

          <CardContent>
            <div className="text-2xl font-bold text-red-600">
              {formatUSDDisplay(toUSD(data.totalLoans, "INR"))}
            </div>
            <div className="text-sm text-muted-foreground">Active Loans</div>
          </CardContent>
        </Card>

        <Card
          className="cursor-pointer hover:shadow-lg transition-all duration-200 hover:scale-105 border-l-4 border-l-blue-500"
          onClick={() => handleNavigation("/dashboard/transfers")}
        >
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Total Transfers</CardTitle>
            <ArrowUpDown className="h-4 w-4 text-blue-600" />
          </CardHeader>

          <CardContent>
            <div className="text-2xl font-bold text-blue-600">
              {formatUSDDisplay(toUSD(data.totalTransfers, "INR"))}
            </div>
            <div className="text-sm text-muted-foreground">Money Sent</div>
          </CardContent>
        </Card>

        <Card
          className="cursor-pointer hover:shadow-lg transition-all duration-200 hover:scale-105 border-l-4 border-l-purple-500"
          onClick={() => handleNavigation("/dashboard/expenses")}
        >
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Total Expenses</CardTitle>
            <ShoppingCart className="h-4 w-4 text-purple-600" />
          </CardHeader>

          <CardContent>
            <div className="text-2xl font-bold text-purple-600">
              {formatUSDDisplay(toUSD(data.totalExpenses, "INR"))}
            </div>
            <div className="text-sm text-muted-foreground">Spending</div>
          </CardContent>
        </Card>
      </div>

      {/* Financial Summary */}
      <div className="grid gap-4 md:grid-cols-3">
        <div className="text-center p-4 bg-green-50 dark:bg-green-950/20 rounded-lg border border-green-200 dark:border-green-800">
          <div className="text-2xl font-bold text-green-600">
            {formatUSDDisplay(toUSD(data.totalIncome, "INR"))}
          </div>
          <div className="text-sm text-muted-foreground">Total Income</div>
        </div>

        <div className="text-center p-4 bg-red-50 dark:bg-red-950/20 rounded-lg border border-red-200 dark:border-red-800">
          <div className="text-2xl font-bold text-red-600">
            {formatUSDDisplay(
              toUSD(data.totalLoans + data.totalExpenses, "INR")
            )}
          </div>
          <div className="text-sm text-muted-foreground">Total Obligations</div>
        </div>

        <div className="text-center p-4 bg-blue-50 dark:bg-blue-950/20 rounded-lg border border-blue-200 dark:border-blue-800">
          <div
            className={`text-2xl font-bold ${
              data.totalIncome - data.totalLoans - data.totalExpenses >= 0
                ? "text-green-600"
                : "text-red-600"
            }`}
          >
            {formatUSDDisplay(
              toUSD(
                data.totalIncome - data.totalLoans - data.totalExpenses,
                "INR"
              )
            )}
          </div>
          <div className="text-sm text-muted-foreground">Net Position</div>
        </div>
      </div>

      {/* Quick Actions */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <Plus className="h-5 w-5" />
            Quick Actions
          </CardTitle>

          <CardDescription>
            Quickly access common financial management tasks
          </CardDescription>
        </CardHeader>

        <CardContent>
          <div className="grid gap-3 md:grid-cols-2 lg:grid-cols-4">
            <Button
              variant="outline"
              className="h-auto p-4 flex flex-col items-center gap-2 hover:bg-green-50 hover:border-green-200 dark:hover:bg-green-950/20 bg-transparent"
              onClick={() => handleNavigation("/dashboard/income")}
            >
              <DollarSign className="h-6 w-6 text-green-600" />
              <div className="text-center">
                <div className="font-medium">Add Income</div>
                <div className="text-xs text-muted-foreground">Record new income</div>
              </div>
            </Button>

            <Button
              variant="outline"
              className="h-auto p-4 flex flex-col items-center gap-2 hover:bg-red-50 hover:border-red-200 dark:hover:bg-red-950/20 bg-transparent"
              onClick={() => handleNavigation("/dashboard/loans")}
            >
              <CreditCard className="h-6 w-6 text-red-600" />
              <div className="text-center">
                <div className="font-medium">Manage Loans</div>
                <div className="text-xs text-muted-foreground">Track your debt</div>
              </div>
            </Button>

            <Button
              variant="outline"
              className="h-auto p-4 flex flex-col items-center gap-2 hover:bg-blue-50 hover:border-blue-200 dark:hover:bg-blue-950/20 bg-transparent"
              onClick={() => handleNavigation("/dashboard/transfers")}
            >
              <Send className="h-6 w-6 text-blue-600" />
              <div className="text-center">
                <div className="font-medium">Send Money</div>
                <div className="text-xs text-muted-foreground">International transfers</div>
              </div>
            </Button>

            <Button
              variant="outline"
              className="h-auto p-4 flex flex-col items-center gap-2 hover:bg-purple-50 hover:border-purple-200 dark:hover:bg-purple-950/20 bg-transparent"
              onClick={() => handleNavigation("/dashboard/expenses")}
            >
              <ShoppingCart className="h-6 w-6 text-purple-600" />
              <div className="text-center">
                <div className="font-medium">Track Expenses</div>
                <div className="text-xs text-muted-foreground">Monthly spending</div>
              </div>
            </Button>

            <Button
              variant="outline"
              className="h-auto p-4 flex flex-col items-center gap-2 hover:bg-orange-50 hover:border-orange-200 dark:hover:bg-orange-950/20 bg-transparent"
              onClick={() => handleNavigation("/dashboard/history")}
            >
              <History className="h-6 w-6 text-orange-600" />
              <div className="text-center">
                <div className="font-medium">View History</div>
                <div className="text-xs text-muted-foreground">Transaction history</div>
              </div>
            </Button>

            <Button
              variant="outline"
              className="h-auto p-4 flex flex-col items-center gap-2 hover:bg-teal-50 hover:border-teal-200 dark:hover:bg-teal-950/20 bg-transparent"
              onClick={() => handleNavigation("/dashboard/analytics")}
            >
              <BarChart3 className="h-6 w-6 text-teal-600" />
              <div className="text-center">
                <div className="font-medium">Analytics</div>
                <div className="text-xs text-muted-foreground">Financial insights</div>
              </div>
            </Button>

            <Button
              variant="outline"
              className="h-auto p-4 flex flex-col items-center gap-2 hover:bg-gray-50 hover:border-gray-200 dark:hover:bg-gray-950/20 bg-transparent"
              onClick={() => handleNavigation("/dashboard/reports")}
            >
              <FileText className="h-6 w-6 text-gray-600" />
              <div className="text-center">
                <div className="font-medium">Reports</div>
                <div className="text-xs text-muted-foreground">Generate reports</div>
              </div>
            </Button>

            <Button
              variant="outline"
              className="h-auto p-4 flex flex-col items-center gap-2 hover:bg-indigo-50 hover:border-indigo-200 dark:hover:bg-indigo-950/20 bg-transparent"
              onClick={() => handleNavigation("/dashboard/payments")}
            >
              <Calendar className="h-6 w-6 text-indigo-600" />
              <div className="text-center">
                <div className="font-medium">Scheduled Payments</div>
                <div className="text-xs text-muted-foreground">Future payments</div>
              </div>
            </Button>
          </div>
        </CardContent>
      </Card>

      {/* Recent Activity */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <History className="h-5 w-5" />
            Recent Activity
          </CardTitle>

          <CardDescription>
            Your latest financial transactions and updates
          </CardDescription>
        </CardHeader>

        <CardContent>
          <div className="space-y-4">
            <div className="flex items-center justify-between p-3 rounded-lg bg-muted/50">
              <div className="flex items-center gap-3">
                <div className="h-8 w-8 rounded-full bg-green-100 dark:bg-green-900/20 flex items-center justify-center">
                  <DollarSign className="h-4 w-4 text-green-600" />
                </div>

                <div>
                  <p className="font-medium">Income Management</p>
                  <p className="text-sm text-muted-foreground">
                    Track earnings with accurate exchange rates
                  </p>
                </div>
              </div>

              <Badge variant="outline">Active</Badge>
            </div>

            <div className="flex items-center justify-between p-3 rounded-lg bg-muted/50">
              <div className="flex items-center gap-3">
                <div className="h-8 w-8 rounded-full bg-red-100 dark:bg-red-900/20 flex items-center justify-center">
                  <CreditCard className="h-4 w-4 text-red-600" />
                </div>

                <div>
                  <p className="font-medium">Loan Tracking</p>
                  <p className="text-sm text-muted-foreground">
                    Monitor debt with payment history
                  </p>
                </div>
              </div>

              <Badge variant="outline">{data.activeLoans} loans</Badge>
            </div>

            <div className="flex items-center justify-between p-3 rounded-lg bg-muted/50">
              <div className="flex items-center gap-3">
                <div className="h-8 w-8 rounded-full bg-blue-100 dark:bg-blue-900/20 flex items-center justify-center">
                  <Send className="h-4 w-4 text-blue-600" />
                </div>

                <div>
                  <p className="font-medium">International Transfers</p>
                  <p className="text-sm text-muted-foreground">
                    Send money globally with accurate rates
                  </p>
                </div>
              </div>

              <Badge variant="outline">Available</Badge>
            </div>

            <div className="flex items-center justify-between p-3 rounded-lg bg-muted/50">
              <div className="flex items-center gap-3">
                <div className="h-8 w-8 rounded-full bg-purple-100 dark:bg-purple-900/20 flex items-center justify-center">
                  <ShoppingCart className="h-4 w-4 text-purple-600" />
                </div>

                <div>
                  <p className="font-medium">Monthly Expense Tracker</p>
                  <p className="text-sm text-muted-foreground">
                    Track current month spending
                  </p>
                </div>
              </div>

              <Badge variant="outline">New</Badge>
            </div>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
