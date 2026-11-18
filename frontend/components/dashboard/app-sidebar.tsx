"use client";

import { useState } from "react";
import { useRouter, usePathname } from "next/navigation";

import {
  useAuth,
  type AuthUser,
} from "@/components/auth/auth-provider";

import {
  Sidebar,
  SidebarContent,
  SidebarFooter,
  SidebarGroup,
  SidebarGroupContent,
  SidebarGroupLabel,
  SidebarHeader,
  SidebarMenu,
  SidebarMenuButton,
  SidebarMenuItem,
  SidebarTrigger,
} from "@/components/ui/sidebar";

import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";

import {
  Avatar,
  AvatarFallback,
  AvatarImage,
} from "@/components/ui/avatar";

import { ThemeToggle } from "@/components/theme-toggle";
import { Separator } from "@/components/ui/separator";

import {
  Home,
  DollarSign,
  TrendingUp,
  CreditCard,
  PiggyBank,
  ArrowUpDown,
  FileText,
  Settings,
  LogOut,
  ChevronUp,
  History,
  PieChart,
  Receipt,
} from "lucide-react";

/**
 * The sidebar receives our own backend AuthUser type.
 *
 * We no longer depend on the Supabase User type.
 */
interface AppSidebarProps {
  user: AuthUser;
}

// Navigation configuration for the dashboard.
const navigationGroups = [
  {
    label: "Overview",
    items: [
      {
        title: "Dashboard",
        url: "/dashboard",
        icon: Home,
      },
    ],
  },
  {
    label: "Financial Management",
    items: [
      {
        title: "Income",
        url: "/dashboard/income",
        icon: TrendingUp,
      },
      {
        title: "Loan Management",
        url: "/dashboard/loans",
        icon: CreditCard,
      },
      {
        title: "Payments",
        url: "/dashboard/payments",
        icon: PiggyBank,
      },
      {
        title: "Transfers",
        url: "/dashboard/transfers",
        icon: ArrowUpDown,
      },
      {
        title: "Transaction History",
        url: "/dashboard/history",
        icon: History,
      },
      {
        title: "Expense Tracker",
        url: "/dashboard/expenses",
        icon: Receipt,
      },
    ],
  },
  {
    label: "Reports",
    items: [
      {
        title: "Analytics",
        url: "/dashboard/analytics",
        icon: PieChart,
      },
      {
        title: "Reports",
        url: "/dashboard/reports",
        icon: FileText,
      },
      {
        title: "Settings",
        url: "/dashboard/settings",
        icon: Settings,
      },
    ],
  },
];

export function AppSidebar({ user }: AppSidebarProps) {
  const router = useRouter();
  const pathname = usePathname();

  // Get the backend sign-out operation from our global auth context.
  const { signOut } = useAuth();

  const [isLoggingOut, setIsLoggingOut] = useState(false);

  /**
   * Sign out through Spring Boot rather than Supabase.
   *
   * The backend invalidates the refresh session and clears its cookie.
   * We navigate to the sign-in page even if the request reports an error,
   * because the AuthProvider clears local authentication state in finally.
   */
  const handleSignOut = async () => {
    if (isLoggingOut) {
      return;
    }

    setIsLoggingOut(true);

    try {
      await signOut();
    } catch (error) {
      console.error("Backend sign-out failed:", error);
    } finally {
      router.replace("/");
      setIsLoggingOut(false);
    }
  };

  // Generate a simple avatar fallback from the email address.
  const getUserInitials = (email: string) => {
    return email.substring(0, 2).toUpperCase();
  };

  // The backend currently supplies an email-derived display name.
  const getUserDisplayName = () => {
    return user.displayName || "User";
  };

  return (
    <Sidebar collapsible="icon" className="border-r">
      <SidebarHeader className="border-b">
        <div className="flex h-16 items-center gap-2 px-4">
          <SidebarTrigger className="-ml-1" />

          <Separator
            orientation="vertical"
            className="mr-2 h-4"
          />

          <div className="flex flex-1 items-center gap-2 group-data-[collapsible=icon]:hidden">
            <div className="flex-shrink-0 rounded-lg bg-primary p-2">
              <DollarSign className="h-5 w-5 text-primary-foreground" />
            </div>

            <div className="flex min-w-0 flex-col">
              <span className="truncate font-semibold text-sidebar-foreground">
                FinanceTracker
              </span>

              <span className="truncate text-xs text-sidebar-foreground/70">
                Personal Finance
              </span>
            </div>
          </div>

          <div className="flex items-center gap-2 group-data-[collapsible=icon]:hidden">
            <ThemeToggle />
          </div>
        </div>
      </SidebarHeader>

      <SidebarContent className="px-2">
        {navigationGroups.map((group) => (
          <SidebarGroup key={group.label}>
            <SidebarGroupLabel className="px-2 py-2 text-xs font-medium text-sidebar-foreground/70 group-data-[collapsible=icon]:hidden">
              {group.label}
            </SidebarGroupLabel>

            <SidebarGroupContent>
              <SidebarMenu>
                {group.items.map((item) => (
                  <SidebarMenuItem key={item.title}>
                    <SidebarMenuButton
                      asChild
                      isActive={pathname === item.url}
                      className="w-full justify-start"
                      tooltip={item.title}
                    >
                      <a
                        href={item.url}
                        className="flex items-center gap-3 px-3 py-2"
                      >
                        <item.icon className="h-4 w-4 flex-shrink-0" />
                        <span className="truncate">
                          {item.title}
                        </span>
                      </a>
                    </SidebarMenuButton>
                  </SidebarMenuItem>
                ))}
              </SidebarMenu>
            </SidebarGroupContent>
          </SidebarGroup>
        ))}
      </SidebarContent>

      <SidebarFooter className="border-t p-2">
        <SidebarMenu>
          <SidebarMenuItem>
            <DropdownMenu>
              <DropdownMenuTrigger asChild>
                <SidebarMenuButton
                  size="lg"
                  className="w-full data-[state=open]:bg-sidebar-accent data-[state=open]:text-sidebar-accent-foreground"
                >
                  <Avatar className="h-8 w-8 flex-shrink-0 rounded-lg">
                    <AvatarImage
                      src={user.avatarUrl || "/placeholder.svg"}
                      alt={getUserDisplayName()}
                    />

                    <AvatarFallback className="rounded-lg text-xs">
                      {getUserInitials(user.email)}
                    </AvatarFallback>
                  </Avatar>

                  <div className="grid min-w-0 flex-1 text-left text-sm leading-tight group-data-[collapsible=icon]:hidden">
                    <span className="truncate font-semibold">
                      {getUserDisplayName()}
                    </span>

                    <span className="truncate text-xs text-sidebar-foreground/70">
                      {user.email}
                    </span>
                  </div>

                  <ChevronUp className="ml-auto size-4 flex-shrink-0 group-data-[collapsible=icon]:hidden" />
                </SidebarMenuButton>
              </DropdownMenuTrigger>

              <DropdownMenuContent
                className="w-[--radix-dropdown-menu-trigger-width] min-w-56 rounded-lg"
                side="bottom"
                align="end"
                sideOffset={4}
              >
                <DropdownMenuItem
                  onClick={handleSignOut}
                  disabled={isLoggingOut}
                  className="cursor-pointer"
                >
                  <LogOut className="mr-2 h-4 w-4" />

                  {isLoggingOut ? "Signing out..." : "Sign out"}
                </DropdownMenuItem>
              </DropdownMenuContent>
            </DropdownMenu>
          </SidebarMenuItem>
        </SidebarMenu>
      </SidebarFooter>
    </Sidebar>
  );
}
