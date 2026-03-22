import { ReactNode } from "react";

interface CardProps {
  children: ReactNode;
  className?: string;
  glow?: boolean;
}

export function Card({ children, className = "", glow = false }: CardProps) {
  return (
    <div
      className={`bg-card border border-border rounded-xl p-5 ${
        glow ? "glow-card" : ""
      } ${className}`}
    >
      {children}
    </div>
  );
}

export function CardTitle({ children, className = "" }: { children: ReactNode; className?: string }) {
  return (
    <h3 className={`text-sm font-semibold text-foreground/60 uppercase tracking-wider mb-3 ${className}`}>
      {children}
    </h3>
  );
}

export function StatCard({
  label,
  value,
  change,
  icon,
}: {
  label: string;
  value: string;
  change?: string;
  icon?: ReactNode;
}) {
  const isPositive = change && !change.startsWith("-");
  return (
    <Card glow>
      <div className="flex items-start justify-between">
        <div>
          <p className="text-xs font-medium text-foreground/40 uppercase tracking-wider">{label}</p>
          <p className="text-2xl font-bold mt-1 text-foreground">{value}</p>
          {change && (
            <p className={`text-sm mt-1 ${isPositive ? "text-green" : "text-red"}`}>
              {isPositive ? "+" : ""}
              {change}
            </p>
          )}
        </div>
        {icon && <div className="text-accent/60">{icon}</div>}
      </div>
    </Card>
  );
}

export function SignalBadge({ signal }: { signal: string }) {
  const cls =
    signal === "STRONG_BUY"
      ? "signal-strong-buy bg-green/10 border-green/20"
      : signal === "BUY"
      ? "signal-buy bg-green/5 border-green/10"
      : signal === "HOLD"
      ? "signal-hold bg-yellow/10 border-yellow/20"
      : signal === "SELL"
      ? "signal-sell bg-red/5 border-red/10"
      : "signal-strong-sell bg-red/10 border-red/20";

  return (
    <span className={`inline-flex items-center px-3 py-1 rounded-full text-xs font-bold border ${cls}`}>
      {signal.replace("_", " ")}
    </span>
  );
}

export function LoadingSpinner() {
  return (
    <div className="flex items-center justify-center p-8">
      <div className="w-8 h-8 border-2 border-accent/20 border-t-accent rounded-full animate-spin" />
    </div>
  );
}

export function EmptyState({ message, icon }: { message: string; icon?: ReactNode }) {
  return (
    <div className="flex flex-col items-center justify-center p-12 text-foreground/30">
      {icon && <div className="mb-3 text-foreground/20">{icon}</div>}
      <p className="text-sm">{message}</p>
    </div>
  );
}
