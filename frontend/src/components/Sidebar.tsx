"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import {
  LayoutDashboard,
  LineChart,
  Brain,
  Sparkles,
  Briefcase,
  Activity,
  ChevronLeft,
  ChevronRight,
  BarChart3,
} from "lucide-react";
import { useState } from "react";

const navItems = [
  { href: "/", label: "Dashboard", icon: LayoutDashboard },
  { href: "/analysis", label: "Stock Analysis", icon: LineChart },
  { href: "/nifty50", label: "Nifty 50 Live", icon: BarChart3 },
  { href: "/ml-pipeline", label: "ML Pipeline", icon: Brain },
  { href: "/advisor", label: "AI Advisor", icon: Sparkles },
  { href: "/trading", label: "Trading", icon: Briefcase },
];

export default function Sidebar() {
  const pathname = usePathname();
  const [collapsed, setCollapsed] = useState(false);

  return (
    <aside
      className={`flex flex-col bg-card border-r border-border transition-all duration-300 ${
        collapsed ? "w-16" : "w-60"
      }`}
    >
      {/* Logo */}
      <div className="flex items-center gap-3 px-4 h-16 border-b border-border">
        <Activity className="w-7 h-7 text-accent shrink-0" />
        {!collapsed && (
          <span className="text-lg font-bold tracking-tight text-accent">
            DSS
          </span>
        )}
      </div>

      {/* Navigation */}
      <nav className="flex-1 py-4 space-y-1 px-2">
        {navItems.map(({ href, label, icon: Icon }) => {
          const active = pathname === href;
          return (
            <Link
              key={href}
              href={href}
              className={`flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium transition-all ${
                active
                  ? "bg-accent/10 text-accent"
                  : "text-foreground/60 hover:text-foreground hover:bg-card-hover"
              }`}
              title={label}
            >
              <Icon className={`w-5 h-5 shrink-0 ${active ? "text-accent" : ""}`} />
              {!collapsed && <span>{label}</span>}
            </Link>
          );
        })}
      </nav>

      {/* Collapse toggle */}
      <button
        onClick={() => setCollapsed(!collapsed)}
        className="flex items-center justify-center h-12 border-t border-border text-foreground/40 hover:text-foreground transition-colors"
      >
        {collapsed ? <ChevronRight className="w-4 h-4" /> : <ChevronLeft className="w-4 h-4" />}
      </button>
    </aside>
  );
}
